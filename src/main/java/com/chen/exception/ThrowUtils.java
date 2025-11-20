package com.chen.exception;

/**
 * ThrowUtils 工具类，用于条件判断并抛出异常
 * 提供了多个重载方法，用于在不同场景下抛出异常
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     * 这是一个基础方法，直接判断条件并抛出指定的运行时异常
     * @param condition        条件
     * @param runtimeException 异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
