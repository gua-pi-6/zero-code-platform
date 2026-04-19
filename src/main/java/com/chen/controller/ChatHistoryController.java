package com.chen.controller;

import com.chen.annotation.AuthCheck;
import com.chen.common.BaseResponse;
import com.chen.common.ResultUtils;
import com.chen.constant.UserConstant;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.dto.chathistory.ChatHistoryQueryRequest;
import com.chen.model.entity.ChatHistory;
import com.chen.model.entity.User;
import com.chen.service.ChatHistoryService;
import com.chen.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 提供聊天历史查询接口。
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 使用游标分页查询单个应用的聊天历史。
     *
     * @param appId 应用 id
     * @param pageSize 分页大小
     * @param lastCreateTime 游标时间
     * @param request Servlet 请求对象
     * @return 分页聊天历史
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        // 先获取当前登录用户，服务层会基于用户信息执行权限校验。
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询聊天历史。
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 分页聊天历史
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 在构造查询条件前，先校验请求参数。
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();

        // 构造查询条件，并执行分页查询。
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }
}
