package com.chen.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑模式请求体。
 */
@Data
public class AppChatEditRequest implements Serializable {

    private Long appId;

    private String message;

    private static final long serialVersionUID = 1L;
}
