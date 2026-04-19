package com.chen.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.chen.constant.AppConstant;
import com.chen.core.builder.VueProjectBuilder;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.mapper.AppMapper;
import com.chen.model.dto.app.AppQueryRequest;
import com.chen.model.entity.App;
import com.chen.model.entity.User;
import com.chen.model.enums.CodeGenTypeEnum;
import com.chen.model.vo.AppVO;
import com.chen.model.vo.UserVO;
import com.chen.service.AppService;
import com.chen.service.AppSummaryService;
import com.chen.service.ChatHistoryService;
import com.chen.service.ScreenshotService;
import com.chen.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提供应用查询、删除、部署和视图组装能力。
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppSummaryService appSummaryService;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    /**
     * 删除应用及其相关衍生数据。
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    @Override
    public boolean removeById(Serializable appId) {
        // 删除前先校验传入的应用 id。
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用 id 为空");
        long deleteAppId = Long.parseLong(appId.toString());
        ThrowUtils.throwIf(deleteAppId <= 0L, ErrorCode.PARAMS_ERROR, "应用 id 错误");

        // 尽力先删除关联的聊天历史。
        try {
            chatHistoryService.deleteByAppId(deleteAppId);
        } catch (Exception e) {
            log.info("删除应用 {} 对话消息失败，原因: {}", deleteAppId, e.getMessage());
        }

        // 尽力继续删除关联的共享摘要。
        try {
            appSummaryService.deleteByAppId(deleteAppId);
        } catch (Exception e) {
            log.info("删除应用 {} 摘要失败，原因: {}", deleteAppId, e.getMessage());
        }

        // 最后再删除应用本身的记录。
        return super.removeById(deleteAppId);
    }

    /**
     * 部署应用的生成产物。
     *
     * @param appId 应用 id
     * @param loginUser 当前登录用户
     * @return 部署地址
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 校验应用 id 和当前登录用户。
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "用户未登录");

        // 部署前先加载应用，并确认当前用户具备所有权。
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限");

        // 优先复用已有 deployKey，不存在时再懒加载生成。
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }

        // 定位应用对应的生成产物目录。
        String codeGenType = app.getCodeGenType();
        String sourceDirPath = codeGenType + "_" + appId;
        String sourcePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirPath;
        File source = new File(sourcePath);
        ThrowUtils.throwIf(!source.exists() || !source.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "原始生成路径不存在");

        // 前端工程在部署前先执行构建，确保发布产物目录可用。
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            vueProjectBuilder.buildProjectAsync(sourcePath);
            File distPath = new File(sourcePath, "dist");
            ThrowUtils.throwIf(!distPath.exists() || !distPath.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "vue 项目构建失败");
            source = distPath;
        }

        // 将最终产物复制到部署目录。
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        FileUtil.copyContent(source, new File(deployDirPath), true);

        // 将部署元数据回写到应用记录中。
        App updateApp = App.builder()
                .id(appId)
                .deployKey(deployKey)
                .deployedTime(LocalDateTime.now())
                .editTime(LocalDateTime.now())
                .build();
        boolean update = this.updateById(updateApp);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新应用信息失败");

        // 部署地址准备完成后，异步触发截图生成。
        String deployUrl = String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
        generateAppScreenshotAsync(appId, deployUrl);
        return deployUrl;
    }

    /**
     * 异步生成并上传应用截图。
     *
     * @param appId 应用 id
     * @param appUrl 应用预览地址
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 先校验截图任务依赖的输入参数。
        ThrowUtils.throwIf(appId == null || appId <= 0L, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(appUrl), ErrorCode.PARAMS_ERROR, "应用访问 url 不能为空");

        // 使用虚拟线程执行截图流程，避免阻塞当前请求链路。
        Thread.startVirtualThread(() -> {
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);

            // 将截图地址回写到应用记录中。
            App updateApp = App.builder()
                    .id(appId)
                    .cover(screenshotUrl)
                    .editTime(LocalDateTime.now())
                    .build();
            boolean update = this.updateById(updateApp);
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新应用封面失败");
        });
    }

    /**
     * 将应用实体转换为视图对象。
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    @Override
    public AppVO getAppVO(App app) {
        // 兼容调用方在条件查询下传入空对象的场景。
        if (app == null) {
            return null;
        }

        // 先复制应用字段，再补充关联的用户视图信息。
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 构造应用列表查询条件。
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        // 尽早拦截空查询请求。
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 提取请求中支持的筛选与排序字段。
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        // 基于提取出的字段组装查询条件。
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType, StrUtil.isNotBlank(codeGenType))
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 将应用实体列表转换为视图对象列表。
     *
     * @param appList 应用实体列表
     * @return 应用视图对象列表
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        // 无数据时直接返回空列表。
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }

        // 先批量查询关联用户，避免循环中重复查询。
        Set<Long> userIds = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 逐个转换应用实体，并挂载缓存好的用户视图。
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }
}
