package com.chen.ai;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

/**
 * 讨论模式专用 AI 接口。
 */
public interface AiDiscussionService {

    @SystemMessage(fromResource = "prompt/codegen-discussion-system-prompt.txt")
    Flux<String> discussStream(String userMessage);
}
