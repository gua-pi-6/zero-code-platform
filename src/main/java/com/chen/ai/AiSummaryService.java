package com.chen.ai;

import dev.langchain4j.service.SystemMessage;

/**
 * 应用共享摘要 AI 接口。
 */
public interface AiSummaryService {

    @SystemMessage(fromResource = "prompt/app-summary-system-prompt.txt")
    String refreshSummary(String summaryInput);
}
