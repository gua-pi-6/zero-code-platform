package com.chen.service;

import com.chen.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chen.model.entity.User;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.chen.model.entity.ChatHistory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author 辰
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    /**
     * 分页查询应用的对话历史
     * @param appId 应用id
     * @param pageSize 分页大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 当前登录用户
     * @return 对话历史分页结果
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 添加对话消息
     * @param message 消息
     * @param userId 用户id
     * @param appId 应用id
     * @param messageType 消息类型
     * @return 是否成功
     */
    boolean addChatMessage(String message, Long userId, Long appId, String messageType);

    /**
     * 根据应用id删除对话历史
     * @param appId 应用id
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 获取查询包装类
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询包装类
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
