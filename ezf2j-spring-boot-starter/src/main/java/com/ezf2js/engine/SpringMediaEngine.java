package com.ezf2js.engine;

import cn.hutool.crypto.digest.MD5;
import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.engine.StreamManager;
import com.ezf2jc.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring 环境下的媒体推流引擎
 * 基于 Spring Bean 的 API 封装
 *
 * @author ZJ
 */
@Slf4j
@Component
public class SpringMediaEngine {

    /**
     * MediaService 实例（由 Spring 注入）
     */
    @Autowired
    private MediaService mediaService;

    /**
     * 引擎配置（由 Spring 注入）
     */
    @Autowired
    private BaseMediaConfig config;

    /**
     * StreamManager 实例（由 Spring 注入）
     */
    @Autowired(required = false)
    private StreamManager streamManager;

    /**
     * 设置 MediaService（用于手动注册）
     */
    public void setMediaService(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * 设置配置（用于手动注册）
     */
    public void setConfig(BaseMediaConfig config) {
        this.config = config;
    }

    /**
     * 设置 StreamManager（用于手动注册）
     */
    public void setStreamManager(StreamManager streamManager) {
        this.streamManager = streamManager;
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
        CameraDto cameraDto = buildCameraDto(streamUrl, useFFmpeg);
        String streamId = mediaService.playForApi(cameraDto);

        if (streamId!= null) {
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
        CameraDto cameraDto = buildCameraDto(streamUrl, useFFmpeg);
        cameraDto.setAutoClose(autoClose);
        cameraDto.setNoClientsDuration(noClientsDuration);

        String streamId = mediaService.playForApi(cameraDto);

        if (streamId!= null) {
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
     * @param host 服务器主机地址（可选，默认使用配置的 host）
     * @param port 服务器端口（可选，默认使用配置的 port）
     * @return WebSocket 播放地址
     */
    public String getWsPlayUrl(String streamUrl, String host, Integer port) {
        String actualHost = host != null ? host : config.getHost();
        int actualPort = port != null ? port : config.getPort();
        return String.format("ws://%s:%d/live?url=%s", actualHost, actualPort, streamUrl);
    }

    /**
     * 生成 HTTP-FLV 播放地址
     * @param streamUrl 原始视频流地址
     * @param host 服务器主机地址（可选）
     * @param port 服务器端口（可选）
     * @return HTTP-FLV 播放地址
     */
    public String getHttpPlayUrl(String streamUrl, String host, Integer port) {
        String actualHost = host != null ? host : config.getHost();
        int actualPort = port != null ? port : config.getPort();
        return String.format("http://%s:%d/live?url=%s", actualHost, actualPort, streamUrl);
    }

    /**
     * 生成 HLS 播放地址
     * @param streamUrl 原始视频流地址
     * @param host 服务器主机地址（可选）
     * @param port 服务器端口（可选）
     * @return HLS 播放地址
     */
    public String getHlsPlayUrl(String streamUrl, String host, Integer port) {
        String actualHost = host != null ? host : config.getHlsConfig().getHost();
        int actualPort = port != null ? port : config.getHlsConfig().getPort();
        return String.format("http://%s:%d/hls/%s/out.m3u8", actualHost, actualPort,
                MD5.create().digestHex(streamUrl));
    }

    /**
     * 获取所有活跃的流
     * @return 流地址数组
     */
    public String[] getAllActiveStreams() {
        if (streamManager != null) {
            List<StreamManager.StreamInfo> allStreams = streamManager.getAllStreams();

            if (allStreams == null || allStreams.isEmpty()) {
                return null;
            }

            return allStreams.stream()
                    .map(StreamManager.StreamInfo::getMediaKey)
                    .toArray(String[]::new);
        }
        return mediaService.getActiveStreams();
    }

    /**
     * 检查流是否存在
     * @param streamUrl 视频流地址
     * @return 是否存在
     */
    public boolean hasStream(String streamUrl) {
        String mediaKey = MD5.create().digestHex(streamUrl);
        if (streamManager != null) {
            return streamManager.getStreamInfo(mediaKey)!= null;
        }
        return mediaService.hasStream(mediaKey);
    }

    /**
     * 获取活跃流数量
     * @return 活跃流数量
     */
    public int getActiveStreamCount() {
        if (streamManager != null) {
            return streamManager.getActiveStreamCount();
        }
        return mediaService.getActiveStreamCount();
    }

    /**
     * 获取客户端总数
     * @return 客户端总数
     */
    public int getTotalClientCount() {
        if (streamManager != null) {
            return streamManager.getTotalClientCount();
        }
        return 0;
    }

    /**
     * 获取 MediaService 实例
     * @return MediaService 实例
     */
    public MediaService getMediaService() {
        return mediaService;
    }

    /**
     * 获取配置对象
     * @return 配置对象
     */
    public BaseMediaConfig getConfig() {
        return config;
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

//    // ... existing code ...
//    /**
//     * 创建 StreamManager Bean
//     * @param mediaService MediaService 实例
//     * @return StreamManager 实例
//     */
//    @Bean
//    @ConditionalOnMissingBean(StreamManager.class)
//    public StreamManager streamManager(MediaService mediaService) {
//        log.debug("创建 StreamManager Bean");
//        return new StreamManager(mediaService);
//    }

//    /**
//     * 创建 SpringMediaEngine Bean（Spring 环境下的推流引擎）
//     * @param mediaService MediaService 实例
//     * @param config 媒体配置
//     * @param streamManager StreamManager 实例（可选）
//     * @return SpringMediaEngine 实例
//     */
//    @Bean
//    @ConditionalOnMissingBean(SpringMediaEngine.class)
//    public SpringMediaEngine springMediaEngine(MediaService mediaService,
//                                               BaseMediaConfig config,
//                                               StreamManager streamManager) {
//        log.info("创建 SpringMediaEngine Bean...");
//
//        SpringMediaEngine engine = new SpringMediaEngine();
//        engine.setMediaService(mediaService);
//        engine.setConfig(config);
//        engine.setStreamManager(streamManager);
//
//        log.info("✅ SpringMediaEngine 初始化完成");
//        return engine;
//    }
//}

}
