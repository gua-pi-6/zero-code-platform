package com.chen.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用聊天模式枚举。
 * 新链路优先使用枚举，而不是散落的字符串常量。
 */
@Getter
public enum AppChatModeEnum {

    EDIT("编辑模式", "edit"),
    CHAT("仅对话模式", "chat");

    private final String text;

    private final String value;

    AppChatModeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static AppChatModeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppChatModeEnum anEnum : AppChatModeEnum.values()) {
            if (anEnum.value.equalsIgnoreCase(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public static AppChatModeEnum getEnumByNameOrValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppChatModeEnum anEnum : AppChatModeEnum.values()) {
            if (anEnum.name().equalsIgnoreCase(value) || anEnum.value.equalsIgnoreCase(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
