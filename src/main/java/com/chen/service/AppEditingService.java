package com.chen.service;

import com.chen.model.entity.User;
import reactor.core.publisher.Flux;

/**
 * 处理编辑模式下的聊天请求。
 */
public interface AppEditingService {

    /**
     * 针对指定应用发起一轮编辑。
     *
     * @param message 用户输入内容
     * @param appId 目标应用 id
     * @param loginUser 当前登录用户
     * @return 流式编辑结果
     */
    Flux<String> edit(String message, Long appId, User loginUser);
}
