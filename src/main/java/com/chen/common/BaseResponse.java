package com.chen.common;

import com.chen.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用基础响应类，用于封装API返回结果
 * @param <T> 泛型，表示返回的数据类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    // 状态码，表示API返回的状态
    private int code;

    // 返回的数据，泛型类型
    private T data;

    // 返回的消息，通常用于描述状态或错误信息
    private String message;

    /**
     * 构造方法，用于创建完整的响应对象
     * @param code 状态码
     * @param data 返回的数据
     * @param message 返回的消息
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 构造方法，用于创建不带消息的响应对象
     * @param code 状态码
     * @param data 返回的数据
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 构造方法，用于创建基于错误码的响应对象
     * @param ErrorCode 错误码枚举，包含状态码和错误信息
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
