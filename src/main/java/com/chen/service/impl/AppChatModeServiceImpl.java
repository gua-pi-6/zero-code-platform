package com.chen.service.impl;

import cn.hutool.core.util.StrUtil;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.exception.ThrowUtils;
import com.chen.model.enums.AppChatModeEnum;
import com.chen.service.AppChatModeService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 在 Redis 中存储并校验聊天模式状态。
 */
@Service
public class AppChatModeServiceImpl implements AppChatModeService {

    private static final String APP_CHAT_MODE_KEY_TEMPLATE = "app:mode:%s:%s";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取指定用户在指定应用下的当前聊天模式。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     * @return 当前聊天模式
     */
    @Override
    public AppChatModeEnum getCurrentMode(Long userId, Long appId) {
        // 读取 Redis 状态前，先校验用户和应用标识。
        validateUserAndApp(userId, appId);
        String modeValue = stringRedisTemplate.opsForValue().get(buildCacheKey(userId, appId));

        // 当还没有显式状态时，默认使用编辑模式。
        if (StrUtil.isBlank(modeValue)) {
            return AppChatModeEnum.EDIT;
        }

        // 解析缓存中的模式值，并拦截非法缓存内容。
        AppChatModeEnum chatModeEnum = AppChatModeEnum.getEnumByNameOrValue(modeValue);
        ThrowUtils.throwIf(chatModeEnum == null, ErrorCode.APP_CHAT_MODE_INVALID, "模式状态非法");
        return chatModeEnum;
    }

    /**
     * 切换指定用户在指定应用下的聊天模式。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     * @param targetMode 目标聊天模式
     */
    @Override
    public void switchMode(Long userId, Long appId, AppChatModeEnum targetMode) {
        // 同时校验身份信息和目标模式是否合法。
        validateUserAndApp(userId, appId);
        ThrowUtils.throwIf(targetMode == null, ErrorCode.APP_CHAT_MODE_INVALID, "目标模式非法");

        // 使用用户和应用维度的 Redis Key 持久化目标模式。
        stringRedisTemplate.opsForValue().set(buildCacheKey(userId, appId), targetMode.getValue(), 12, TimeUnit.HOURS);
    }

    /**
     * 校验当前应用模式是否允许进入编辑态。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     */
    @Override
    public void assertEditable(Long userId, Long appId) {
        // 先读取当前模式，若不是编辑模式则立即失败。
        AppChatModeEnum currentMode = getCurrentMode(userId, appId);
        if (!AppChatModeEnum.EDIT.equals(currentMode)) {
            throw new BusinessException(ErrorCode.APP_EDIT_MODE_REQUIRED, "当前模式不允许编辑，请先切回编辑模式");
        }
    }

    /**
     * 校验模式操作依赖的用户 id 和应用 id。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     */
    private void validateUserAndApp(Long userId, Long appId) {
        // 在访问存储前，先拦截缺失或非法的用户、应用标识。
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不存在");
    }

    /**
     * 构造当前模式使用的 Redis Key。
     *
     * @param userId 当前用户 id
     * @param appId 当前应用 id
     * @return Redis Key
     */
    private String buildCacheKey(Long userId, Long appId) {
        // 让 Key 同时绑定用户和应用，避免不同上下文互相冲突。
        return APP_CHAT_MODE_KEY_TEMPLATE.formatted(userId, appId);
    }
}
