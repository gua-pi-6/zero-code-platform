package com.chen.service;

import com.chen.model.enums.AppChatModeEnum;

/**
 * 维护用户在指定应用下的当前聊天模式。
 */
public interface AppChatModeService {

    /**
     * 获取指定用户在指定应用下的当前聊天模式。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     * @return 当前聊天模式
     */
    AppChatModeEnum getCurrentMode(Long userId, Long appId);

    /**
     * 切换指定用户在指定应用下的聊天模式。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     * @param targetMode 目标聊天模式
     */
    void switchMode(Long userId, Long appId, AppChatModeEnum targetMode);

    /**
     * 校验当前模式是否允许进入编辑链路。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     */
    void assertEditable(Long userId, Long appId);
}
