package com.chen.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     * 用于标识具体的业务错误类型
     */
    private final int code;

    /**
     * 构造方法1
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造方法2
     * @param ErrorCode 错误码枚举，包含错误码和错误信息
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造方法3
     * @param ErrorCode 错误码枚举，包含错误码
     * @param message 自定义错误信息，会覆盖枚举中的默认错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
