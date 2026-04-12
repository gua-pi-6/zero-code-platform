package com.chen.exception;

import lombok.Getter;

/**
 * 错误码枚举类
 * 用于定义系统中可能出现的各种错误情况及其对应的错误码和错误信息
 * 使用@Getter注解自动为枚举值生成getter方法
 */
@Getter
public enum ErrorCode {


    // 成功状态
    SUCCESS(0, "ok"),                          // 成功状态码


    // 客户端错误（4xx）
    PARAMS_ERROR(40000, "请求参数错误"),         // 请求参数错误
    NOT_LOGIN_ERROR(40100, "未登录"),           // 未登录错误
    NO_AUTH_ERROR(40101, "无权限"),             // 无权限错误
    NOT_FOUND_ERROR(40400, "请求数据不存在"),    // 资源不存在错误
    TOO_MANY_REQUEST(42900, "请求过于频繁"),
    FORBIDDEN_ERROR(40300, "禁止访问"),         // 禁止访问错误


    // 服务器错误（5xx）
    SYSTEM_ERROR(50000, "系统内部异常"),         // 系统内部错误
    OPERATION_ERROR(50001, "操作失败");         // 操作失败错误

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
