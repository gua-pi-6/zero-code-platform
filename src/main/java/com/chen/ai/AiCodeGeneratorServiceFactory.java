package com.chen.ai;

import com.chen.ai.guardrail.PromptSafetyInputGuardrail;
import com.chen.ai.tools.ToolManager;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.model.enums.CodeGenTypeEnum;
import com.chen.service.ChatHistoryService;
import com.chen.utils.SpringContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 编辑模式代码生成工厂。
 * 这里只负责 edit 链路，discussion 已经拆到独立工厂。
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    private final Cache<String, AiCodeGeneratorService> aiServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(30))
            .removalListener((key, val, removalCause) ->
                    log.info("cache removed, key: {}, removalCause: {}", key, removalCause))
            .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        return aiServiceCache.get(appId.toString(), key -> createAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML));
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return aiServiceCache.get(createCacheKey(appId, codeGenTypeEnum),
                key -> createAiCodeGeneratorService(appId, codeGenTypeEnum));
    }

    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        // edit memory 与 discussion memory 分开存，避免修改上下文污染讨论上下文。
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .id(createMemoryId(appId))
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(100)
                .build();
        chatHistoryService.loadChatHistory(appId, windowChatMemory, 20, AppChatModeEnum.EDIT.getValue());

        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> {
                // 只有编辑 vue 项目时才允许工具调用。
                StreamingChatModel reasoningStreamingChatModel =
                        SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> windowChatMemory)
                        .tools(toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: There is no tool named " + toolExecutionRequest.name()))
                        .build();
            }
            case HTML, MULTI_FILE -> {
                // 纯文本代码生成不需要 tools，保持链路简单。
                StreamingChatModel openAiStreamingChatModel =
                        SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .chatMemory(windowChatMemory)
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "不支持的代码生成类型 " + codeGenTypeEnum.getValue());
        };
    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(1L, CodeGenTypeEnum.HTML);
    }

    private String createCacheKey(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return appId + "_" + codeGenTypeEnum.getValue();
    }

    private String createMemoryId(Long appId) {
        return "app:" + appId + ":" + AppChatModeEnum.EDIT.getValue();
    }
}
