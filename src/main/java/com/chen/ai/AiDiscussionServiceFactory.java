package com.chen.ai;

import com.chen.ai.guardrail.PromptSafetyInputGuardrail;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.service.ChatHistoryService;
import com.chen.utils.SpringContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 讨论模式 AI 工厂。
 * 这里故意不注册 tools，保证 discussion 只能输出文本讨论结果。
 */
@Configuration
@Slf4j
public class AiDiscussionServiceFactory {

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    private final Cache<String, AiDiscussionService> aiServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(30))
            .removalListener((key, val, removalCause) ->
                    log.info("discussion service cache removed, key: {}, removalCause: {}", key, removalCause))
            .build();

    public AiDiscussionService getAiDiscussionService(Long appId) {
        return aiServiceCache.get(createCacheKey(appId), key -> createAiDiscussionService(appId));
    }

    private AiDiscussionService createAiDiscussionService(Long appId) {
        // chat memory 与 edit memory 分开存，避免两条链路串上下文。
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .id(createMemoryId(appId))
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(100)
                .build();
        chatHistoryService.loadChatHistory(appId, windowChatMemory, 20, AppChatModeEnum.CHAT.getValue());

        StreamingChatModel reasoningStreamingChatModel =
                SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiDiscussionService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> windowChatMemory)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    private String createCacheKey(Long appId) {
        return "discussion_" + appId;
    }

    private String createMemoryId(Long appId) {
        return "app:" + appId + ":" + AppChatModeEnum.CHAT.getValue();
    }
}
