package com.chen.service.impl;

import cn.hutool.core.util.StrUtil;
import com.chen.constant.UserConstant;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.mapper.ChatHistoryMapper;
import com.chen.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chen.model.entity.App;
import com.chen.model.entity.ChatHistory;
import com.chen.model.entity.User;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.chen.service.AppService;
import com.chen.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 持久化并查询聊天历史，同时保持 edit 和 chat 模式隔离。
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryServiceImpl.class);

    @Resource
    @Lazy
    private AppService appService;

    /**
     * 将最近聊天历史加载到指定的聊天记忆窗口中。
     *
     * @param appId 应用 id
     * @param messageWindowChatMemory 目标记忆窗口
     * @param maxMessageCount 最大加载消息数
     * @param chatMode 目标聊天模式
     * @return 实际加载的消息数量
     */
    @Override
    public int loadChatHistory(Long appId, MessageWindowChatMemory messageWindowChatMemory, Integer maxMessageCount, String chatMode) {
        try {
            // 查询历史前先校验所有必要输入。
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
            ThrowUtils.throwIf(messageWindowChatMemory == null, ErrorCode.PARAMS_ERROR, "消息窗口不能为空");
            ThrowUtils.throwIf(maxMessageCount == null || maxMessageCount <= 0, ErrorCode.PARAMS_ERROR, "最大消息数量不能为空");
            ThrowUtils.throwIf(AppChatModeEnum.getEnumByValue(chatMode) == null, ErrorCode.APP_CHAT_MODE_INVALID, "对话模式错误");

            // 仅加载目标应用、目标模式下的最近记录。
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .eq(ChatHistory::getChatMode, chatMode)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxMessageCount);
            List<ChatHistory> chatHistories = this.list(queryWrapper);
            ThrowUtils.throwIf(chatHistories == null || chatHistories.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "对话历史不存在");

            // 反转记录顺序，确保按时间正序回放到记忆中。
            chatHistories = chatHistories.reversed();

            // 先清空目标记忆，再按顺序回放 AI 和用户消息。
            int loadCount = 0;
            messageWindowChatMemory.clear();
            for (ChatHistory chatHistory : chatHistories) {
                if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                    messageWindowChatMemory.add(AiMessage.from(chatHistory.getMessage()));
                    loadCount++;
                }
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                    messageWindowChatMemory.add(UserMessage.from(chatHistory.getMessage()));
                    loadCount++;
                }
            }
            return loadCount;
        } catch (Exception e) {
            // 加载历史属于尽力而为逻辑，不应影响主请求流程。
            log.info("加载对话历史失败 appId: {} 错误信息: {}", appId, e.getMessage());
            return 0;
        }
    }

    /**
     * 分页查询单个应用的聊天历史。
     *
     * @param appId 应用 id
     * @param pageSize 分页大小
     * @param lastCreateTime 游标时间
     * @param loginUser 当前登录用户
     * @return 分页聊天历史
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        // 校验基础分页参数和当前登录用户。
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在 1-50 之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 加载应用，并校验当前用户是否有权限查看历史。
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");

        // 构造游标查询条件，并执行第一页查询。
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 保存一条聊天历史消息。
     *
     * @param message 消息内容
     * @param userId 用户 id
     * @param appId 应用 id
     * @param messageType 消息类型
     * @param chatMode 聊天模式
     * @param sessionId 会话 id
     * @return 是否保存成功
     */
    @Override
    public boolean addChatMessage(String message, Long userId, Long appId, String messageType, String chatMode, String sessionId) {
        // 持久化前先校验消息负载是否合法。
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(ChatHistoryMessageTypeEnum.getEnumByValue(messageType) == null, ErrorCode.PARAMS_ERROR, "消息类型不存在");
        ThrowUtils.throwIf(AppChatModeEnum.getEnumByValue(chatMode) == null, ErrorCode.APP_CHAT_MODE_INVALID, "对话模式错误");
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "未登录");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");

        // 连同模式和会话信息一起持久化保存消息。
        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(messageType)
                .chatMode(chatMode)
                .sessionId(sessionId)
                .appId(appId)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    /**
     * 删除单个应用下的全部聊天历史记录。
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        // 构造仅作用于目标应用的删除条件。
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 构造聊天历史查询条件。
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 调用方未传筛选条件时，直接返回空查询包装器。
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }

        // 提取支持的筛选与排序字段。
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();

        // 先组装基础筛选条件。
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);

        // 调用方传入游标时间时，追加游标边界条件。
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }

        // 优先使用显式排序，否则默认按最新历史优先排序。
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }
}
