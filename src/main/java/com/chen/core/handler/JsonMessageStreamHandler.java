package com.chen.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chen.ai.model.message.AiResponseMessage;
import com.chen.ai.model.message.StreamMessage;
import com.chen.ai.model.message.StreamMessageTypeEnum;
import com.chen.ai.model.message.ToolExecutedMessage;
import com.chen.ai.model.message.ToolRequestMessage;
import com.chen.ai.tools.BaseTool;
import com.chen.ai.tools.ToolManager;
import com.chen.model.entity.User;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.chen.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器，仅用于 edit + vue_project。
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private ToolManager toolManager;

    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId,
                               User loginUser,
                               String chatMode,
                               String sessionId) {
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds))
                .filter(StrUtil::isNotBlank)
                .doOnComplete(() -> {
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(aiResponse, loginUser.getId(), appId,
                            ChatHistoryMessageTypeEnum.AI.getValue(), chatMode, sessionId);
                })
                .doOnError(error -> {
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(errorMessage, loginUser.getId(), appId,
                            ChatHistoryMessageTypeEnum.AI.getValue(), chatMode, sessionId);
                });
    }

    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            log.error("不支持的消息类型: {}", chunk);
            return "";
        }
        return switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                chatHistoryStringBuilder.append(data);
                yield data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 同一个 tool request 可能被拆成多段流，这里只在第一次向前端展示。
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    seenToolIds.add(toolId);
                    BaseTool tool = toolManager.getTool(toolName);
                    yield tool == null ? "" : tool.generateToolRequestResponse();
                }
                yield "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                BaseTool tool = toolManager.getTool(toolName);
                if (tool == null) {
                    yield "";
                }
                String result = tool.generateToolExecutedResult(jsonObject);
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                yield output;
            }
        };
    }
}
