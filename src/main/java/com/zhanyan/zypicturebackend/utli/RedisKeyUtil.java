package com.zhanyan.zypicturebackend.utli;

import com.zhanyan.zypicturebackend.constant.PictureConstant;

public class RedisKeyUtil {

    /**  
     * 获取 缓存 key
     */  
    public static String getPictureCacheKey(String className, String key) {
        String tempPictureKeyPrefix = PictureConstant.TEMP_PICTURE_KEY_PREFIX;
        return String.format(tempPictureKeyPrefix, className, key);
    }
}
