package com.chen.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 模式切换请求体。
 */
@Data
public class AppChatModeSwitchRequest implements Serializable {

    private Long appId;

    private String targetMode;

    private static final long serialVersionUID = 1L;
}
