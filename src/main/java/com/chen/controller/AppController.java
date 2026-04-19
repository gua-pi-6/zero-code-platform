package com.chen.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.chen.ai.AiCodeGenTypeRoutingServiceFactory;
import com.chen.ai.AiSummaryService;
import com.chen.annotation.AuthCheck;
import com.chen.common.BaseResponse;
import com.chen.common.DeleteRequest;
import com.chen.common.ResultUtils;
import com.chen.constant.AppConstant;
import com.chen.constant.UserConstant;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.dto.app.AppAddRequest;
import com.chen.model.dto.app.AppAdminUpdateRequest;
import com.chen.model.dto.app.AppChatDiscussRequest;
import com.chen.model.dto.app.AppChatEditRequest;
import com.chen.model.dto.app.AppChatModeSwitchRequest;
import com.chen.model.dto.app.AppDeployRequest;
import com.chen.model.dto.app.AppQueryRequest;
import com.chen.model.dto.app.AppUpdateRequest;
import com.chen.model.entity.App;
import com.chen.model.entity.User;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.model.enums.CodeGenTypeEnum;
import com.chen.model.vo.AppVO;
import com.chen.ratelimiter.annotation.RateLimit;
import com.chen.ratelimiter.enums.RateLimitType;
import com.chen.service.AppChatModeService;
import com.chen.service.AppDiscussionService;
import com.chen.service.AppEditingService;
import com.chen.service.AppService;
import com.chen.service.ProjectDownloadService;
import com.chen.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 提供应用的增删改查、部署和聊天相关接口。
 * 聊天接口已拆分为 discuss / edit / mode-switch 三类入口。
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private AppDiscussionService appDiscussionService;

    @Resource
    private AppEditingService appEditingService;

    @Resource
    private AppChatModeService appChatModeService;

    @Resource
    private AiSummaryService aiSummaryService;

    /**
     * 下载指定应用的生成代码。
     *
     * @param appId 应用 id
     * @param request Servlet 请求对象
     * @param response Servlet 响应对象
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId, HttpServletRequest request, HttpServletResponse response) {
        // 先校验应用 id，并确认目标应用存在。
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 无效");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 只有应用所有者才允许下载生成后的源码包。
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }

        // 根据应用类型和应用 id 组装生成代码目录。
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");

        // 将代码目录以 zip 压缩包形式输出。
        String downloadFileName = String.valueOf(appId);
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

    /**
     * 部署已生成的应用，并返回预览地址。
     *
     * @param appDeployRequest 部署请求
     * @param request Servlet 请求对象
     * @return 部署地址
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 在进入部署链路前，先校验请求参数。
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");

        // 获取当前登录用户，并委托服务层执行部署。
        User loginUser = userService.getLoginUser(request);
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 发起一轮仅讨论模式的聊天。
     *
     * @param discussRequest 讨论请求
     * @param request Servlet 请求对象
     * @return SSE 响应流
     */
    @PostMapping(value = "/chat/discuss", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(rate = 8, rateInterval = 60, limitType = RateLimitType.USER, message = "用户请求频率过快，请稍后重试")
    public Flux<ServerSentEvent<String>> discuss(@RequestBody AppChatDiscussRequest discussRequest, HttpServletRequest request) {
        // 校验请求体以及基础字段。
        ThrowUtils.throwIf(discussRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = discussRequest.getAppId();
        String message = discussRequest.getMessage();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");

        // 委托讨论服务处理，并统一包装为 SSE 事件流。
        User loginUser = userService.getLoginUser(request);
        Flux<String> result = appDiscussionService.discuss(message, appId, loginUser);
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "讨论失败");
        return buildSseResponse(result);
    }

    /**
     * 发起一轮编辑模式的聊天。
     *
     * @param editRequest 编辑请求
     * @param request Servlet 请求对象
     * @return SSE 响应流
     */
    @PostMapping(value = "/chat/edit", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(rate = 8, rateInterval = 60, limitType = RateLimitType.USER, message = "用户请求频率过快，请稍后重试")
    public Flux<ServerSentEvent<String>> edit(@RequestBody AppChatEditRequest editRequest, HttpServletRequest request) {
        // 校验请求体以及基础字段。
        ThrowUtils.throwIf(editRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = editRequest.getAppId();
        String message = editRequest.getMessage();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");

        // 委托编辑服务处理，并统一包装为 SSE 事件流。
        User loginUser = userService.getLoginUser(request);
        Flux<String> result = appEditingService.edit(message, appId, loginUser);
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "编辑失败");
        return buildSseResponse(result);
    }

    /**
     * 切换当前应用的聊天模式。
     *
     * @param switchRequest 模式切换请求
     * @param request Servlet 请求对象
     * @return 是否切换成功
     */
    @PostMapping("/chat/mode/switch")
    public BaseResponse<Boolean> switchChatMode(@RequestBody AppChatModeSwitchRequest switchRequest, HttpServletRequest request) {
        // 校验请求体以及目标模式是否合法。
        ThrowUtils.throwIf(switchRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = switchRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
        AppChatModeEnum targetMode = AppChatModeEnum.getEnumByNameOrValue(switchRequest.getTargetMode());
        ThrowUtils.throwIf(targetMode == null, ErrorCode.APP_CHAT_MODE_INVALID, "目标模式非法");

        // 只有应用所有者才能修改该应用保存的模式状态。
        User loginUser = userService.getLoginUser(request);
        validateOwnedApp(appId, loginUser);
        appChatModeService.switchMode(loginUser.getId(), appId, targetMode);
        return ResultUtils.success(true);
    }

    /**
     * 创建应用。
     *
     * @param appAddRequest 创建请求
     * @param request Servlet 请求对象
     * @return 新应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        // 在构造应用实体前，先校验初始提示词。
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");

        // 结合请求参数和当前用户构造应用实体。
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        app.setAppName(aiSummaryService.summaryAppName(initPrompt));

        // 根据初始提示词路由出最合适的代码生成类型。
        CodeGenTypeEnum codeGenType = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService().routeCodeGenType(initPrompt);
        app.setCodeGenType(codeGenType.getValue());
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    /**
     * 更新当前用户拥有的应用。
     *
     * @param appUpdateRequest 更新请求
     * @param request Servlet 请求对象
     * @return 是否更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 先校验请求参数。
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 加载目标应用，并校验当前用户是否有权限操作。
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 仅更新允许编辑的字段。
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除当前用户拥有的应用，管理员也允许执行删除。
     *
     * @param deleteRequest 删除请求
     * @param request Servlet 请求对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 先校验请求参数。
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 仅允许应用所有者或管理员删除应用。
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用视图对象。
     *
     * @param id 应用 id
     * @return 应用视图对象
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        // 校验应用 id，并加载目标应用。
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询当前用户的应用列表。
     *
     * @param appQueryRequest 查询请求
     * @param request Servlet 请求对象
     * @return 分页应用视图列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        // 校验请求参数以及分页大小限制。
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();

        // 将查询范围限制为当前登录用户自己的应用。
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 返回前将实体列表转换为视图对象列表。
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选应用列表。
     *
     * @param appQueryRequest 查询请求
     * @return 分页应用视图列表
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "goodAppPage",
            key = "T(com.chen.utils.CacheKeyUtils).generateCacheKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        // 校验请求参数以及分页大小限制。
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();

        // 强制查询精选应用数据
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 返回前将实体列表转换为视图对象列表。
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员删除应用。
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        // 校验请求参数，并确认目标应用存在。
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用。
     *
     * @param appAdminUpdateRequest 管理员更新请求
     * @return 是否更新成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        // 校验请求参数，并确认目标应用存在。
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        // 复制允许更新的字段，并持久化管理员更新结果。
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页查询应用列表。
     *
     * @param appQueryRequest 查询请求
     * @return 分页应用视图列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        // 校验请求参数，并执行管理员查询。
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 返回前将实体列表转换为视图对象列表。
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用视图对象。
     *
     * @param id 应用 id
     * @return 应用视图对象
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        // 校验应用 id，并加载目标应用。
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 将原始字符串流包装为前端约定的 SSE 格式。
     *
     * @param resultFlux 原始消息流
     * @return SSE 事件流
     */
    private Flux<ServerSentEvent<String>> buildSseResponse(Flux<String> resultFlux) {
        return resultFlux
                .map(chunk -> {
                    // 保持现有负载结构不变，确保前端仍然从 d 字段取值。
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonStr = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(jsonStr)
                            .build();
                })
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("")
                        .build()));
    }

    /**
     * 校验当前用户是否拥有目标应用。
     *
     * @param appId 应用 id
     * @param loginUser 当前登录用户
     */
    private void validateOwnedApp(Long appId, User loginUser) {
        // 先加载应用，再校验当前用户是否具备所有者权限。
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
    }
}
