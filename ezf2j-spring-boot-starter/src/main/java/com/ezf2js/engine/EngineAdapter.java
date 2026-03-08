package com.ezf2js.engine;

import com.ezf2jc.engine.MediaEngine;
import com.ezf2jc.loader.MediaServerLoader;
import com.ezf2js.engine.SpringMediaEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 引擎适配器
 * 兼容 Spring 和非 Spring 环境
 *
 * @author ZJ
 */
@Slf4j
@Component
public class EngineAdapter {

    /**
     * Spring 环境下的引擎（可选）
     */
    @Autowired(required = false)
    private SpringMediaEngine springEngine;

    /**
     * 是否运行在 Spring 环境
     * @return 是否在 Spring 环境
     */
    public boolean isSpringEnvironment() {
        return springEngine != null;
    }

    /**
     * 开启推流（自动适配环境）
     * @param streamUrl 视频流地址
     * @param useFFmpeg 是否使用 FFmpeg
     * @return 推流是否成功
     */
    public String startStream(String streamUrl, boolean useFFmpeg) {
        if (isSpringEnvironment()) {
            log.debug("使用 Spring 环境推流");
            return springEngine.startStream(streamUrl, useFFmpeg);
        } else {
            log.debug("使用非 Spring 环境推流");
            MediaEngine engine = MediaServerLoader.getMediaEngine();
            return engine.startStream(streamUrl, useFFmpeg);
        }
    }

    /**
     * 关闭推流（自动适配环境）
     * @param streamUrl 视频流地址
     */
    public void stopStream(String streamUrl) {
        if (isSpringEnvironment()) {
            log.debug("使用 Spring 环境关闭推流");
            springEngine.stopStream(streamUrl);
        } else {
            log.debug("使用非 Spring 环境关闭推流");
            MediaEngine engine = MediaServerLoader.getMediaEngine();
            engine.stopStream(streamUrl);
        }
    }

    /**
     * 生成 WebSocket 播放地址（自动适配环境）
     * @param streamUrl 原始视频流地址
     * @param host 主机地址
     * @param port 端口号
     * @return WebSocket 播放地址
     */
    public String getWsPlayUrl(String streamUrl, String host, int port) {
        if (isSpringEnvironment()) {
            return springEngine.getWsPlayUrl(streamUrl, host, port);
        } else {
            MediaEngine engine = MediaServerLoader.getMediaEngine();
            return engine.getWsPlayUrl(streamUrl, host, port);
        }
    }

    /**
     * 获取活跃流数量（自动适配环境）
     * @return 活跃流数量
     */
    public int getActiveStreamCount() {
        if (isSpringEnvironment()) {
            return springEngine.getActiveStreamCount();
        } else {
            MediaEngine engine = MediaServerLoader.getMediaEngine();
            return engine.getMediaService().getActiveStreamCount();
        }
    }
}
