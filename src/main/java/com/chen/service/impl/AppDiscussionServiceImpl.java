package com.chen.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.chen.ai.AiDiscussionServiceFactory;
import com.chen.core.handler.StreamHandlerExecutor;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.entity.App;
import com.chen.model.entity.User;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.chen.service.AppDiscussionService;
import com.chen.service.AppService;
import com.chen.service.AppSummaryService;
import com.chen.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 处理仅讨论模式的聊天轮次，不进入代码保存链路。
 */
@Service
@Slf4j
public class AppDiscussionServiceImpl implements AppDiscussionService {

    @Resource
    private AppService appService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private AiDiscussionServiceFactory aiDiscussionServiceFactory;

    @Resource
    private AppSummaryService appSummaryService;

    /**
     * 为当前用户和应用发起一轮讨论。
     *
     * @param message 用户输入内容
     * @param appId 目标应用 id
     * @param loginUser 当前登录用户
     * @return 流式讨论结果
     */
    @Override
    public Flux<String> discuss(String message, Long appId, User loginUser) {
        // 先校验基础输入参数。
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 确保当前用户拥有目标应用。
        ensureAppOwner(appId, loginUser);

        // 为当前轮生成新的 sessionId，并保存用户消息
        String sessionId = IdUtil.fastSimpleUUID();
        chatHistoryService.addChatMessage(message, loginUser.getId(), appId,
                ChatHistoryMessageTypeEnum.USER.getValue(), AppChatModeEnum.CHAT.getValue(), sessionId);

        // 拼接共享摘要，让讨论模式能够承接跨模式上下文。
        String aiInput = buildAiInput(appId, message);
        Flux<String> aiMessage = aiDiscussionServiceFactory.getAiDiscussionService(appId).discussStream(aiInput);

        // 使用流处理执行器在讨论结束后统一落库 AI 输出。
        return streamHandlerExecutor.doExecute(aiMessage, chatHistoryService, appId, loginUser,
                        null, AppChatModeEnum.CHAT.getValue(), sessionId)
                .doOnComplete(() -> refreshSummaryQuietly(appId));
    }

    /**
     * 校验当前用户是否拥有目标应用。
     *
     * @param appId 目标应用 id
     * @param loginUser 当前登录用户
     */
    private void ensureAppOwner(Long appId, User loginUser) {
        // 在执行历史记录或 AI 调用前，先加载应用并校验所有权。
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
    }

    /**
     * 在存在共享摘要时，拼接最终发送给 AI 的输入内容。
     *
     * @param appId 目标应用 id
     * @param userMessage 用户消息
     * @return 最终 AI 输入
     */
    private String buildAiInput(Long appId, String userMessage) {
        // 先读取共享摘要，便于讨论模式延续之前的结论。
        String summaryContent = appSummaryService.getSummaryContent(appId);
        if (StrUtil.isBlank(summaryContent)) {
            return userMessage;
        }
        return """
                共享摘要（用于跨模式延续上下文，仅作参考）：
                %s

                用户最新消息：
                %s
                """.formatted(summaryContent, userMessage);
    }

    /**
     * 在一轮讨论结束后静默刷新共享摘要。
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
