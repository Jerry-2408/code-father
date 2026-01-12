package com.example.codefather.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/**
 * 缓存Key生成工具类
 */
public class CacheKeyUtils {

    public static String generateKey(Object obj) {
        // 防止缓存穿透
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 先转为JSON字符串，再生成MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}
