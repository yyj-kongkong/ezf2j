package com.ezf2jc.dto;

import cn.hutool.core.util.RandomUtil;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stream ID 管理器
 * 负责生成和管理随机的流 ID，实现视频源地址与播放地址的隔离
 *
 * @author ZJ
 */
public class StreamIdManager {

    /**
     * Stream ID -> MediaKey 映射关系
     */
    private static final ConcurrentHashMap<String, String> STREAM_ID_TO_MEDIA_KEY = new ConcurrentHashMap<>();

    /**
     * MediaKey -> Stream ID 映射关系（反向索引）
     */
    private static final ConcurrentHashMap<String, String> MEDIA_KEY_TO_STREAM_ID = new ConcurrentHashMap<>();

    /**
     * Stream ID 长度（6 位字母数字组合）
     */
    private static final int STREAM_ID_LENGTH = 6;

    /**
     * 字符集（去除易混淆字符：0/O, 1/I/L）
     */
    private static final String CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";

    /**
     * 生成唯一的 Stream ID
     * @param mediaKey 媒体 Key（内部真实标识）
     * @return Stream ID
     */
    public static synchronized String generateStreamId(String mediaKey) {
        // 检查是否已经存在
        String existingStreamId = MEDIA_KEY_TO_STREAM_ID.get(mediaKey);
        if (existingStreamId != null) {
            return existingStreamId;
        }

        // 生成新的 Stream ID
        String streamId;
        int maxAttempts = 1000;
        int attempts = 0;

        do {
            UUID uuid = UUID.randomUUID();
            streamId = uuid.toString().replace("-", "");
            attempts++;

            if (attempts >= maxAttempts) {
                throw new RuntimeException("生成 Stream ID 失败，超过最大尝试次数");
            }
        } while (STREAM_ID_TO_MEDIA_KEY.containsKey(streamId));

        // 建立映射关系
        STREAM_ID_TO_MEDIA_KEY.put(streamId, mediaKey);
        MEDIA_KEY_TO_STREAM_ID.put(mediaKey, streamId);

        return streamId;
    }

    /**
     * 根据 Stream ID 获取 MediaKey
     * @param streamId Stream ID
     * @return MediaKey，如果不存在返回 null
     */
    public static String getMediaKeyByStreamId(String streamId) {
        return STREAM_ID_TO_MEDIA_KEY.get(streamId);
    }

    /**
     * 根据 MediaKey 获取 Stream ID
     * @param mediaKey MediaKey
     * @return Stream ID，如果不存在返回 null
     */
    public static String getStreamIdByMediaKey(String mediaKey) {
        return MEDIA_KEY_TO_STREAM_ID.get(mediaKey);
    }

    /**
     * 检查 Stream ID 是否存在
     * @param streamId Stream ID
     * @return 是否存在
     */
    public static boolean hasStreamId(String streamId) {
        return STREAM_ID_TO_MEDIA_KEY.containsKey(streamId);
    }

    /**
     * 删除 Stream ID 映射关系
     * @param mediaKey MediaKey
     */
    public static void removeMapping(String mediaKey) {
        String streamId = MEDIA_KEY_TO_STREAM_ID.remove(mediaKey);
        if (streamId != null) {
            STREAM_ID_TO_MEDIA_KEY.remove(streamId);
        }
    }

    /**
     * 清除所有映射关系
     */
    public static void clearAll() {
        STREAM_ID_TO_MEDIA_KEY.clear();
        MEDIA_KEY_TO_STREAM_ID.clear();
    }

    /**
     * 获取当前活跃的 Stream ID 数量
     * @return 数量
     */
    public static int getActiveCount() {
        return STREAM_ID_TO_MEDIA_KEY.size();
    }
}
