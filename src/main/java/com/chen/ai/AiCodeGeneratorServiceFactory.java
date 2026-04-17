package com.chen.ai;

import com.chen.ai.guardrail.PromptSafetyInputGuardrail;
import com.chen.ai.tools.*;
import com.chen.constant.AppConstant;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
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



    /**
     * 创建 Caffeine 并缓存 AI 代码生成和聊天服务
     *
     */
    Cache<String, AiCodeGeneratorService> aiServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            //expireAfterWrite和expireAfterAccess同时存在时，以expireAfterWrite为准
            //最后一次写操作后经过指定时间过期
            .expireAfterWrite(Duration.ofMinutes(30))
            //最后一次读或写操作后经过指定时间过期
            .expireAfterAccess(Duration.ofMinutes(30))
            //监听缓存被移除
            .removalListener((key, val, removalCause) -> {
                log.info("缓存被移除，key: {}, removalCause: {}", key, removalCause);
            })
            .build();

    /**
     * 获取 appId 生成服务 (为了维护老逻辑, 默认返回HTML 项目生成服务)
     *
     * @param appId 应用 ID
     * @return AI 代码生成服务
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        return aiServiceCache.get(appId.toString(), key -> createAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML));
    }

    /**
     * 获取 appId 和 codeGenTypeEnum 对应的应用生成服务
     *
     * @param appId 应用 ID
     * @param codeGenTypeEnum 生成类型
     * @return AI 代码生成服务
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return aiServiceCache.get(this.createCacheKey(appId, codeGenTypeEnum), key -> createAiCodeGeneratorService(appId, codeGenTypeEnum));
    }

    /**
     * 获取 appId 和 codeGenTypeEnum 对应的聊天生成服务
     *
     * @param appId 应用 ID
     * @param codeGenTypeEnum 生成类型
     * @return AI 代码生成服务
     */
    public AiCodeGeneratorService getAiDiscussionGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return aiServiceCache.get(this.createDiscussionCacheKey(appId, codeGenTypeEnum), key -> createAiDiscussionGeneratorService(appId));
    }

    /**
     * 获取 appId 生成聊天服务
     *
     * @param appId 应用 ID
     * @return AI 聊天服务
     */
    private AiCodeGeneratorService createAiDiscussionGeneratorService(Long appId) {
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory
                .builder()
                .id(appId + ":" + AppConstant.CHAT_MODE)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(100)
                .build();
        chatHistoryService.loadChatHistory(appId, windowChatMemory, 20, AppConstant.CHAT_MODE);

        StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> windowChatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
    }


    /**
     * 获取 appId 生成应用服务
     *
     * @param appId 应用 ID
     * @return AI 代码生成服务
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory
                .builder()
                .id(appId + ":" + AppConstant.EDIT_MODE)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(100)
                .build();
        chatHistoryService.loadChatHistory(appId, windowChatMemory, 20, AppConstant.EDIT_MODE);

        return switch (codeGenTypeEnum) {
            // Vue 项目生成 使用 工具调用 和 流式推理模型
            case VUE_PROJECT -> {
                // 设置多例模式解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> windowChatMemory)
                        .tools(toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        // 设置幻觉工具名称策略，避免模型调用不存在的工具时出现异常
                        .hallucinatedToolNameStrategy(
                                toolExecutionRequest -> ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: There is no tool named " + toolExecutionRequest.name()))
                        .build();
            }
            // HTML 和 多文件项目生成 使用 普通流式模型
            case HTML, MULTI_FILE -> {
                // 设置多例模式解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .chatMemory(windowChatMemory)
                        .build();
            }
            default ->
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "不支持的代码生成类型 " + codeGenTypeEnum.getValue());
        };


    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(1L, CodeGenTypeEnum.HTML);
    }

    private String createCacheKey(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return appId + "_" + codeGenTypeEnum.getValue();
    }

    private String createDiscussionCacheKey(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return appId + "_" + codeGenTypeEnum.getValue() + "_discussion";
    }


}
