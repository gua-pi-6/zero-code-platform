package com.chen.service.impl;

import cn.hutool.core.util.StrUtil;
import com.chen.constant.UserConstant;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chen.model.entity.App;
import com.chen.model.entity.User;
import com.chen.model.enums.ChatHistoryMessageTypeEnum;
import com.chen.service.AppService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.chen.model.entity.ChatHistory;
import com.chen.mapper.ChatHistoryMapper;
import com.chen.service.ChatHistoryService;
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
 * 对话历史 服务层实现。
 *
 * @author 辰
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryServiceImpl.class);
    @Resource
    @Lazy
    private AppService appService;




    @Override
    public int loadChatHistory(Long appId, MessageWindowChatMemory messageWindowChatMemory, Integer maxMessageCount) {
        try {
            // 效验参数
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
            ThrowUtils.throwIf(messageWindowChatMemory == null, ErrorCode.PARAMS_ERROR, "消息窗口不能为空");
            ThrowUtils.throwIf(maxMessageCount == null || maxMessageCount <= 0, ErrorCode.PARAMS_ERROR, "最大消息数量不能为空");

            // 构建查询条件
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxMessageCount);

            // 查询对话历史记录
            List<ChatHistory> chatHistories = this.list(queryWrapper);
            ThrowUtils.throwIf(chatHistories == null || chatHistories.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "对话历史不存在");

            // 反转消息列表,确保先展现最早的消息
            chatHistories = chatHistories.reversed();

            // 清空缓存, 并缓存消息, 确保没有多余的消息
            int loadCount = 0;
            messageWindowChatMemory.clear();
            for (ChatHistory chatHistory : chatHistories) {
                if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())){
                    messageWindowChatMemory.add(AiMessage.from(chatHistory.getMessage()));
                    loadCount++;
                }
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())){
                    messageWindowChatMemory.add(UserMessage.from(chatHistory.getMessage()));
                    loadCount++;
                }
            }

            // 返回缓存成功的消息数量
            return loadCount;
        } catch (Exception e) {
            // 失败也不影响系统运行
            log.info("加载对话历史失败 appId: {} 错误信息: {}", appId, e.getMessage());
            return 0;
        }
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }



    @Override
    public boolean addChatMessage(String message, Long userId, Long appId, String messageType) {
        // 效验参数
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(ChatHistoryMessageTypeEnum.getEnumByValue(messageType) == null, ErrorCode.PARAMS_ERROR, "消息类型不存在");
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "未登录");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id不存在");

        // 构建对话历史
        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(messageType)
                .appId(appId)
                .userId(userId)
                .build();

        // 新增对话历史
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }


}
