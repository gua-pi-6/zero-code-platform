package com.chen.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.chen.constant.AppConstant;
import com.chen.core.AiGenerateFacade;
import com.chen.core.builder.VueProjectBuilder;
import com.chen.core.handler.StreamHandlerExecutor;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.dto.app.AppQueryRequest;
import com.chen.model.entity.User;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.chen.model.enums.CodeGenTypeEnum;
import com.chen.model.vo.AppVO;
import com.chen.model.vo.UserVO;
import com.chen.service.ChatHistoryService;
import com.chen.service.ScreenshotService;
import com.chen.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.chen.model.entity.App;
import com.chen.mapper.AppMapper;
import com.chen.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author 辰
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AiGenerateFacade aiGenerateFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;


    @Override
    public boolean removeById(Serializable appId) {
        // 参数效验
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用id为空");
        long deleteAppId = Long.parseLong(appId.toString());
        ThrowUtils.throwIf(deleteAppId <= 0L, ErrorCode.PARAMS_ERROR, "应用id错误");
        // 删除应用的对话消息
        try {
            chatHistoryService.removeById(deleteAppId);
        } catch (Exception e) {
            log.info("删除应用 {} 对话消息失败 失败原因: {}", deleteAppId, e.getMessage());
        }

        // 删除应用
        return super.removeById(deleteAppId);
    }

    @Override
    public Flux<String> chatToGenCode(String message, Long appId, User loginUser) {
        // 效验参数
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "对话消息为空");
        ThrowUtils.throwIf(appId == null || appId <= 0L, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "未登录");
        // 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 用户权限效验 判断是否为应用创建者
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限");
        // 调用 AI 模型生成代码
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "代码生成类型不存在");

        // 保存用户对话历史
        chatHistoryService.addChatMessage(message, loginUser.getId(), appId, ChatHistoryMessageTypeEnum.USER.getValue());

        // 调用 AI 模型生成代码
        Flux<String> aiMessage = aiGenerateFacade.generateWithSaveStream(message, codeGenTypeEnum, appId);

        // 收集 AI 响应的内容, 并保存到对话历史中
        return streamHandlerExecutor.doExecute(aiMessage, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 效验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
        // 校验登录用户
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "用户未登录");
        // 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 用户权限效验 判断是否为应用创建者
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限");
        // 检查是否有deployKey 若没有则生成一个6位deployKey (字母 + 数字)
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 获取代码生成类型 获取原始生成路径
        String codeGenType = app.getCodeGenType();
        String sourceDirPath = codeGenType + "_" + appId;
        String sourcePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirPath;
        // 检验原始生成路径是否存在 (访问路径)
        File source = new File(sourcePath);
        ThrowUtils.throwIf(!source.exists() || !source.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "原始生成路径不存在");

        // 检查是否为 vue 项目
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // 构建vue项目
            vueProjectBuilder.buildProjectAsync(sourcePath);

            // 检查 dist 目录 是否存在
            File distPath = new File(sourcePath, "dist");
            ThrowUtils.throwIf(!distPath.exists() || !distPath.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "vue项目构建失败");

            source = distPath;
        }

        // 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        FileUtil.copyContent(source, new File(deployDirPath), true);
        // 更新应用信息
        App updateApp = App.builder()
                .id(appId)
                .deployKey(deployKey)
                .deployedTime(LocalDateTime.now())
                .editTime(LocalDateTime.now())
                .build();
        boolean update = this.updateById(updateApp);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新应用信息失败");
        // 返回可访问的 URL 地址
        String deployUrl = String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);

        // 异步生成应用封面,并更新应用封面信息
        generateAppScreenshotAsync(appId, deployUrl);
        return deployUrl;
    }

    /**
     * 异步生成应用封面,并更新应用封面信息
     *
     * @param appId        应用id
     * @param appUrl 应用封面访问url
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 参数效验
        ThrowUtils.throwIf(appId == null || appId <= 0L, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(StrUtil.isBlank(appUrl), ErrorCode.PARAMS_ERROR, "应用访问url不能为空");

        // 异步生成封面截图
        Thread.startVirtualThread(() -> {
            // 生成封面截图
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);

            // 更新应用封面信息
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
     * 获取应用视图对象
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 获取应用查询包装器
     *
     * @param appQueryRequest 应用查询请求
     * @return 应用查询包装器
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
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
     * 获取应用视图对象列表
     *
     * @param appList 应用实体列表
     * @return 应用视图对象列表
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }


}
