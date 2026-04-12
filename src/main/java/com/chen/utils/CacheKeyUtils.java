package com.chen.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

public class CacheKeyUtils {

    /**
     * 生成缓存键
     *
     * @param object 缓存对象
     * @return 缓存键
     */
    public static String generateCacheKey(Object object) {
        /**
         * 如果对象为空，直接返回 null 字符串的 MD5 哈希值
         */
        if (object == null) {
            return DigestUtil.md5Hex("null");
        }

        /**
         * 将对象转换为 JSON 字符串
         */
        String jsonStr = JSONUtil.toJsonStr(object);
        /**
         * 对 JSON 字符串进行 MD5 哈希计算，作为缓存键
         */
        return DigestUtil.md5Hex(jsonStr);
    }
}
