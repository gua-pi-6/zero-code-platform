package com.chen.ai;

import com.chen.ai.guardrail.PromptSafetyInputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 共享摘要 AI 工厂。
 */
@Configuration
public class AiSummaryServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    public AiSummaryService createAiSummaryService() {
        return AiServices.builder(AiSummaryService.class)
                .chatModel(chatModel)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    @Bean
    public AiSummaryService aiSummaryService() {
        return createAiSummaryService();
    }
}
