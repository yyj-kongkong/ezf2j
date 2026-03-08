package com.ezf2js.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

/**
 * 媒体服务器环境配置
 * 提供 Environment 工具方法
 *
 * @author ZJ
 */
@Slf4j
public class MediaServerEnvironment {

    /**
     * Spring Environment 实例
     */
    private final Environment environment;

    /**
     * 构造函数
     * @param environment Spring Environment
     */
    public MediaServerEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 获取配置的端口号
     * @return 端口号
     */
    public int getPort() {
        return environment.getProperty("ezf2j.media.port", Integer.class, 8866);
    }

    /**
     * 获取配置的主机地址
     * @return 主机地址
     */
    public String getHost() {
        return environment.getProperty("ezf2j.media.host", "0.0.0.0");
    }

    /**
     * 检查是否启用媒体服务
     * @return 是否启用
     */
    public boolean isEnabled() {
        return environment.getProperty("ezf2j.media.enabled", Boolean.class, true);
    }

    /**
     * 检查是否启用 FFmpeg
     * @return 是否启用 FFmpeg
     */
    public boolean isFFmpegEnabled() {
        return environment.getProperty("ezf2j.media.enable-ffmpeg", Boolean.class, false);
    }

    /**
     * 获取 FFmpeg 路径
     * @return FFmpeg 路径
     */
    public String getFfmpegPath() {
        return environment.getProperty("ezf2j.media.ffmpeg-path");
    }

    /**
     * 获取无人观看关闭时长
     * @return 关闭时长（毫秒）
     */
    public long getNoClientsDuration() {
        return environment.getProperty("ezf2j.media.no-clients-duration", Long.class, 60000L);
    }

    /**
     * 获取 HLS 端口
     * @return HLS 端口
     */
    public int getHlsPort() {
        return environment.getProperty("ezf2j.media.hls.port", Integer.class, 8866);
    }

    /**
     * 获取 HLS 主机
     * @return HLS 主机
     */
    public String getHlsHost() {
        return environment.getProperty("ezf2j.media.hls.host", "localhost");
    }

    /**
     * 打印当前配置信息
     */
    public void printConfiguration() {
        log.info("=== 媒体服务器配置 ===");
        log.info("启用状态：{}", isEnabled());
        log.info("监听地址：{}:{}", getHost(), getPort());
        log.info("FFmpeg 启用：{}", isFFmpegEnabled());
        log.info("FFmpeg 路径：{}", getFfmpegPath() != null ? getFfmpegPath() : "自动查找");
        log.info("自动关闭：true");
        log.info("无人观看关闭时长：{}ms", getNoClientsDuration());
        log.info("HLS 地址：http://{}:{}/hls", getHlsHost(), getHlsPort());
        log.info("======================");
    }
}
