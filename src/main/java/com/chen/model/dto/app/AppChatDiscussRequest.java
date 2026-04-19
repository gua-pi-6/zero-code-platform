package com.chen.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 仅对话模式请求体。
 */
@Data
public class AppChatDiscussRequest implements Serializable {

    private Long appId;

    private String message;

    private static final long serialVersionUID = 1L;
}
