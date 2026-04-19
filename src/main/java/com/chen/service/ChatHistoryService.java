package com.chen.service;

import com.chen.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chen.model.entity.ChatHistory;
import com.chen.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 管理持久化的聊天历史记录。
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 将最近聊天记录加载到指定的聊天记忆窗口中。
     *
     * @param appId 应用 id
     * @param messageWindowChatMemory 目标记忆窗口
     * @param maxMessageCount 最大加载消息数
     * @param chatMode 目标聊天模式
     * @return 实际加载的消息数量
     */
    int loadChatHistory(Long appId, MessageWindowChatMemory messageWindowChatMemory, Integer maxMessageCount, String chatMode);

    /**
     * 分页查询指定应用的聊天历史。
     *
     * @param appId 应用 id
     * @param pageSize 分页大小
     * @param lastCreateTime 游标时间
     * @param loginUser 当前登录用户
     * @return 分页聊天历史
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    /**
     * 持久化保存一条带模式和会话信息的聊天消息。
     *
     * @param message 消息内容
     * @param userId 用户 id
     * @param appId 应用 id
     * @param messageType 消息类型
     * @param chatMode 聊天模式
     * @param sessionId 会话 id
     * @return 是否保存成功
     */
    boolean addChatMessage(String message, Long userId, Long appId, String messageType, String chatMode, String sessionId);

    /**
     * 在未显式提供会话 id 时保存聊天消息。
     *
     * @param message 消息内容
     * @param userId 用户 id
     * @param appId 应用 id
     * @param messageType 消息类型
     * @param chatMode 聊天模式
     * @return 是否保存成功
     */
    default boolean addChatMessage(String message, Long userId, Long appId, String messageType, String chatMode) {
        // 调用完整重载方法，并将会话 id 置为空。
        return addChatMessage(message, userId, appId, messageType, chatMode, null);
    }

    /**
     * 删除指定应用下的全部聊天历史。
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 构造聊天历史查询条件。
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
