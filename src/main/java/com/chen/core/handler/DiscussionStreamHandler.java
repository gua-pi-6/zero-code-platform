package com.chen.core.handler;

import com.chen.model.entity.User;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.chen.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * discussion 流处理器。
 * 这里只做文本聚合和历史落库，不解析工具消息。
 */
@Slf4j
@Component
public class DiscussionStreamHandler {

    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId,
                               User loginUser,
                               String chatMode,
                               String sessionId) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(aiResponse, loginUser.getId(), appId,
                            ChatHistoryMessageTypeEnum.AI.getValue(), chatMode, sessionId);
                })
                .doOnError(error -> {
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(errorMessage, loginUser.getId(), appId,
                            ChatHistoryMessageTypeEnum.AI.getValue(), chatMode, sessionId);
                });
    }
}
