package com.ezf2jc.service;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.digest.MD5;
import com.ezf2jc.common.CacheMap;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.thread.MediaTransferHls;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HLS 服务
 * 处理 HLS 切片和缓存
 *
 * @author ZJ
 */
@Slf4j
public class HlsService {

    /**
     * 默认端口
     */
    private static int defaultPort = 8866;

    /**
     * 设置默认端口
     * @param port 端口号
     */
    public static void setDefaultPort(int port) {
        HlsService.defaultPort = port;
    }

    /**
     * 获取默认端口
     * @return 端口号
     */
    public static int getDefaultPort() {
        return defaultPort;
    }

    /**
     * HLS 转换器缓存
     */
    public static ConcurrentHashMap<String, MediaTransferHls> cameras = new ConcurrentHashMap<>();

    /**
     * TS 缓存（10 秒过期）
     */
    public static CacheMap<String, byte[]> cacheTs = new CacheMap<>(10000);

    /**
     * M3U8 缓存（10 秒过期）
     */
    public static CacheMap<String, byte[]> cacheM3u8 = new CacheMap<>(10000);

    /**
     * 保存 TS 数据
     * @param mediaKey 媒体 Key
     * @param tsName TS 文件名
     * @param in 输入流
     */
    public void processTs(String mediaKey, String tsName, InputStream in) {
        try {
            byte[] readBytes = IoUtil.readBytes(in);
            String tsKey = mediaKey.concat("-").concat(tsName);
            cacheTs.put(tsKey, readBytes);
            log.debug("保存 TS: {}", tsKey);
        } catch (Exception e) {
            log.error("保存 TS 失败", e);
        }
    }

    /**
     * 保存 M3U8 数据
     * @param mediaKey 媒体 Key
     * @param in 输入流
     */
    public void processHls(String mediaKey, InputStream in) {
        try {
            byte[] readBytes = IoUtil.readBytes(in);
            cacheM3u8.put(mediaKey, readBytes);
            log.debug("保存 M3U8: {}", mediaKey);
        } catch (Exception e) {
            log.error("保存 M3U8 失败", e);
        }
    }

    /**
     * 关闭 HLS 切片
     * @param cameraDto 相机 DTO
     */
    public void closeConvertToHls(CameraDto cameraDto) {
        // 生成媒体 Key
        String mediaKey = MD5.create().digestHex(cameraDto.getUrl());

        if (cameras.containsKey(mediaKey)) {
            MediaTransferHls mediaTransferHls = cameras.get(mediaKey);
            mediaTransferHls.stop();
            cameras.remove(mediaKey);
            cacheTs.remove(mediaKey);
            cacheM3u8.remove(mediaKey);
            log.info("已关闭 HLS 切片：{}", mediaKey);
        }
    }

    /**
     * 开始 HLS 切片
     * @param cameraDto 相机 DTO
     * @return 是否成功
     */
    public boolean startConvertToHls(CameraDto cameraDto) {
        // 生成媒体 Key
        String mediaKey = MD5.create().digestHex(cameraDto.getUrl());
        cameraDto.setMediaKey(mediaKey);

        MediaTransferHls mediaTransferHls = cameras.get(mediaKey);

        if (null == mediaTransferHls) {
            log.info("创建 HLS 切片线程");
            mediaTransferHls = new MediaTransferHls(cameraDto, defaultPort);
            cameras.put(mediaKey, mediaTransferHls);
            mediaTransferHls.execute();
        }

        mediaTransferHls = cameras.get(mediaKey);

        // 15 秒还没 true 认为启动不了
        for (int i = 0; i < 30; i++) {
            if (mediaTransferHls.isRunning()) {
                log.info("HLS 切片线程启动成功");
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.error("HLS 切片线程启动超时");
        return false;
    }

    /**
     * 获取 TS 数据
     * @param mediaKey 媒体 Key
     * @param tsName TS 文件名
     * @return TS 数据
     */
    public byte[] getTs(String mediaKey, String tsName) {
        String tsKey = mediaKey.concat("-").concat(tsName);
        return cacheTs.get(tsKey);
    }

    /**
     * 获取 M3U8 数据
     * @param mediaKey 媒体 Key
     * @return M3U8 数据
     */
    public byte[] getM3u8(String mediaKey) {
        return cacheM3u8.get(mediaKey);
    }

    /**
     * 检查 HLS 切片是否运行中
     * @param mediaKey 媒体 Key
     * @return 是否运行中
     */
    public boolean isHlsRunning(String mediaKey) {
        MediaTransferHls hls = cameras.get(mediaKey);
        return hls != null && hls.isRunning();
    }

    /**
     * 获取活跃的 HLS 切片数量
     * @return HLS 切片数量
     */
    public int getActiveHlsCount() {
        return cameras.size();
    }
}
