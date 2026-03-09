package com.ezf2jc.engine;

import cn.hutool.crypto.digest.MD5;
import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.loader.MediaServerLoader;
import com.ezf2jc.service.MediaService;
import lombok.extern.slf4j.Slf4j;

/**
 * 媒体推流引擎
 * 提供统一的 API 进行推流控制
 *
 * @author ZJ
 */
@Slf4j
public class MediaEngine {

    /**
     * MediaService 实例
     */
    private final MediaService mediaService;

    /**
     * 引擎配置
     */
    private final BaseMediaConfig config;

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    /**
     * 构造函数（通过 Loader 创建）
     * @param mediaService MediaService 实例
     * @param config 配置对象
     */
    public MediaEngine(MediaService mediaService, BaseMediaConfig config) {
        this.mediaService = mediaService;
        this.config = config;
        this.initialized = true;
    }

    /**
     * 开启推流（最简单的方式）
     * @param streamUrl 视频流地址（rtsp/rtmp/本地文件）
     * @return 推流是否成功
     */
    public String startStream(String streamUrl) {
        return startStream(streamUrl, false);
    }

    /**
     * 开启推流（可指定转码方式）
     * @param streamUrl 视频流地址
     * @param useFFmpeg 是否使用 FFmpeg（false=JavaCV）
     * @return 推流是否成功
     */
    public String startStream(String streamUrl, boolean useFFmpeg) {
        checkInitialized();

        CameraDto cameraDto = buildCameraDto(streamUrl, useFFmpeg);
        String streamId = mediaService.playForApi(cameraDto);

        if (streamId!=null) {
            log.info("✅ 推流启动成功：{}", streamUrl);
        } else {
            log.error("❌ 推流启动失败：{}", streamUrl);
        }

        return streamId;
    }

    /**
     * 开启推流（完整配置）
     * @param streamUrl 视频流地址
     * @param useFFmpeg 是否使用 FFmpeg
     * @param autoClose 无人观看是否自动关闭
     * @param noClientsDuration 无人观看多久后关闭（毫秒）
     * @return 推流是否成功
     */
    public String startStream(String streamUrl, boolean useFFmpeg,
                               boolean autoClose, long noClientsDuration) {
        checkInitialized();

        CameraDto cameraDto = buildCameraDto(streamUrl, useFFmpeg);
        cameraDto.setAutoClose(autoClose);
        cameraDto.setNoClientsDuration(noClientsDuration);

        String streamId = mediaService.playForApi(cameraDto);

        if (streamId!=null) {
            log.info("✅ 推流启动成功：{} (自动关闭:{}ms)", streamUrl, noClientsDuration);
        } else {
            log.error("❌ 推流启动失败：{}", streamUrl);
        }

        return streamId;
    }

    /**
     * 关闭推流
     * @param streamUrl 视频流地址
     */
    public void stopStream(String streamUrl) {
        checkInitialized();

        String mediaKey = MD5.create().digestHex(streamUrl);
        CameraDto cameraDto = new CameraDto();
        cameraDto.setUrl(streamUrl);
        cameraDto.setMediaKey(mediaKey);

        mediaService.closeForApi(cameraDto);
        log.info("🛑 已关闭推流：{}", streamUrl);
    }

    /**
     * 生成 WebSocket 播放地址
     * @param streamUrl 原始视频流地址
     * @param host 服务器主机地址
     * @param port 服务器端口
     * @return WebSocket 播放地址
     */
    public String getWsPlayUrl(String streamUrl, String host, int port) {
        return String.format("ws://%s:%d/live?url=%s", host, port, streamUrl);
    }

    /**
     * 生成 HTTP-FLV 播放地址
     * @param streamUrl 原始视频流地址
     * @param host 服务器主机地址
     * @param port 服务器端口
     * @return HTTP-FLV 播放地址
     */
    public String getHttpPlayUrl(String streamUrl, String host, int port) {
        return String.format("http://%s:%d/live?url=%s", host, port, streamUrl);
    }

    /**
     * 检查引擎是否已初始化
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("MediaEngine 未初始化，请先调用 MediaServerLoader.load()");
        }
        if (mediaService == null) {
            throw new IllegalStateException("MediaService 未注入");
        }
    }

    /**
     * 构建 CameraDto
     */
    private CameraDto buildCameraDto(String streamUrl, boolean useFFmpeg) {
        String mediaKey = MD5.create().digestHex(streamUrl);
        CameraDto cameraDto = new CameraDto();
        cameraDto.setUrl(streamUrl);
        cameraDto.setMediaKey(mediaKey);
        cameraDto.setEnabledFFmpeg(useFFmpeg);

        // 判断是否为本地文件
        if (isLocalFile(streamUrl)) {
            cameraDto.setType(1);
        }

        return cameraDto;
    }

    /**
     * 判断是否为本地文件
     */
    private boolean isLocalFile(String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) {
            return false;
        }

        String[] split = streamUrl.trim().split("\\:");
        if (split.length > 0 && split[0].length() <= 1) {
            return true;
        }
        return false;
    }

    /**
     * 获取 MediaService 实例（高级用法）
     * @return MediaService 实例
     */
    public MediaService getMediaService() {
        return mediaService;
    }

    /**
     * 获取引擎配置
     * @return 配置对象
     */
    public BaseMediaConfig getConfig() {
        return config;
    }

    /**
     * 检查引擎状态
     * @return 是否运行中
     */
    public boolean isRunning() {
        return initialized && mediaService != null;
    }
}
