package com.chen.core.handler;

import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.entity.User;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.model.enums.CodeGenTypeEnum;
import com.chen.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理分发器。
 * 先按聊天模式分，再按代码生成类型分，避免 discussion 误入编辑链路。
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private DiscussionStreamHandler discussionStreamHandler;

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    @Resource
    private SimpleTextStreamHandler simpleTextStreamHandler;

    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId,
                                  User loginUser,
                                  CodeGenTypeEnum codeGenType,
                                  String chatMode,
                                  String sessionId) {
        AppChatModeEnum chatModeEnum = AppChatModeEnum.getEnumByValue(chatMode);
        ThrowUtils.throwIf(chatModeEnum == null, ErrorCode.APP_CHAT_MODE_INVALID, "对话模式错误");
        // 讨论模式直接短路到 DiscussionStreamHandler，彻底隔离工具和保存逻辑。
        if (AppChatModeEnum.CHAT.equals(chatModeEnum)) {
            return discussionStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser, chatMode, sessionId);
        }
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.PARAMS_ERROR, "代码生成类型不存在");
        return switch (codeGenType) {
            case VUE_PROJECT ->
                    jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser, chatMode, sessionId);
            case HTML, MULTI_FILE ->
                    simpleTextStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser, chatMode, sessionId);
        };
    }
}
