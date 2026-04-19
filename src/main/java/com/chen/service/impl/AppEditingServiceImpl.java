package com.chen.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.chen.core.AiGenerateFacade;
import com.chen.core.handler.StreamHandlerExecutor;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.entity.App;
import com.chen.model.entity.User;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.chen.model.enums.CodeGenTypeEnum;
import com.chen.service.AppChatModeService;
import com.chen.service.AppEditingService;
import com.chen.service.AppService;
import com.chen.service.AppSummaryService;
import com.chen.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 处理编辑模式的聊天轮次，并接入代码生成链路。
 */
@Service
@Slf4j
public class AppEditingServiceImpl implements AppEditingService {

    @Resource
    private AppService appService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private AiGenerateFacade aiGenerateFacade;

    @Resource
    private AppChatModeService appChatModeService;

    @Resource
    private AppSummaryService appSummaryService;

    /**
     * 为当前用户和应用发起一轮编辑。
     *
     * @param message 用户输入内容
     * @param appId 目标应用 id
     * @param loginUser 当前登录用户
     * @return 流式编辑结果
     */
    @Override
    public Flux<String> edit(String message, Long appId, User loginUser) {
        // 先校验基础输入参数。
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 加载应用、校验所有权，并确认当前模式允许编辑。
        App app = getAuthorizedApp(appId, loginUser);
        appChatModeService.assertEditable(loginUser.getId(), appId);

        // 在进入保存链路前，先解析代码生成类型。
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "代码生成类型不存在");

        // 为当前轮生成新的 sessionId，并保存用户消息。
        String sessionId = IdUtil.fastSimpleUUID();
        chatHistoryService.addChatMessage(message, loginUser.getId(), appId,
                ChatHistoryMessageTypeEnum.USER.getValue(), AppChatModeEnum.EDIT.getValue(), sessionId);

        // 拼接共享摘要，让编辑模式能够承接跨模式上下文。
        String aiInput = buildAiInput(appId, message);
        Flux<String> aiMessage = aiGenerateFacade.generateWithSaveStream(aiInput, codeGenTypeEnum, appId);

        // 使用流处理执行器在编辑结束后统一落库 AI 输出。
        return streamHandlerExecutor.doExecute(aiMessage, chatHistoryService, appId, loginUser,
                        codeGenTypeEnum, AppChatModeEnum.EDIT.getValue(), sessionId)
                .doOnComplete(() -> refreshSummaryQuietly(appId));
    }

    /**
     * 加载目标应用，并校验当前用户是否拥有该应用。
     *
     * @param appId 目标应用 id
     * @param loginUser 当前登录用户
     * @return 已通过权限校验的应用
     */
    private App getAuthorizedApp(Long appId, User loginUser) {
        // 在执行生成链路前，先加载应用并校验所有权。
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
        return app;
    }

    /**
     * 在存在共享摘要时，拼接最终发送给 AI 的输入内容。
     *
     * @param appId 目标应用 id
     * @param userMessage 用户消息
     * @return 最终 AI 输入
     */
    private String buildAiInput(Long appId, String userMessage) {
        // 先读取共享摘要，便于编辑模式延续之前的结论。
        String summaryContent = appSummaryService.getSummaryContent(appId);
        if (StrUtil.isBlank(summaryContent)) {
            return userMessage;
        }
        return """
                之前对话总结的上下文（用于跨模式延续上下文，仅作参考）：
                %s

                用户最新消息：
                %s
                """.formatted(summaryContent, userMessage);
    }

    /**
     * 在一轮编辑结束后静默刷新共享摘要。
     *
     * @param appId 目标应用 id
     */
    private void refreshSummaryQuietly(Long appId) {
        try {
            // 以轮次为单位刷新摘要，避免按 token 频繁更新。
            appSummaryService.refreshSummaryIncrementally(appId);
        } catch (Exception e) {
            log.warn("刷新应用摘要失败 appId={}, error={}", appId, e.getMessage());
        }
    }
}
