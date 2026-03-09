package com.ezf2js.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

@Slf4j
public class MediaServerEnvironment {

    private final Environment environment;

    public MediaServerEnvironment(Environment environment) {
        this.environment = environment;
    }

    public int getPort() {
        return environment.getProperty("ezf2j.media.port", Integer.class, 53251);
    }

    public String getServerName() {
        return environment.getProperty("ezf2j.media.server-name", "EZ-F2J Media Server");
    }

    public String getFlvPath() {
        return environment.getProperty("ezf2j.media.flv-path", "/live");
    }

    public String getHlsPath() {
        return environment.getProperty("ezf2j.media.hls-path", "/hls");
    }

    public boolean isEnabled() {
        return environment.getProperty("ezf2j.media.enabled", Boolean.class, true);
    }

    public boolean isFFmpegEnabled() {
        return environment.getProperty("ezf2j.media.enable-ffmpeg", Boolean.class, false);
    }

    public boolean isHlsEnabled() {
        return environment.getProperty("ezf2j.media.enable-hls", Boolean.class, false);
    }

    public String getFfmpegPath() {
        return environment.getProperty("ezf2j.media.ffmpeg-path");
    }

    public long getNoClientsDuration() {
        return environment.getProperty("ezf2j.media.no-clients-duration", Long.class, 60000L);
    }

    public boolean isAutoClose() {
        return environment.getProperty("ezf2j.media.auto-close", Boolean.class, true);
    }

    public void printConfiguration() {
        log.info("=== EZF2J 媒体服务器配置 ===");
        log.info("启用状态：{}", isEnabled());
        log.info("服务名称：{}", getServerName());
        log.info("监听地址：0.0.0.0:{}", getPort());
        log.info("FLV 路径：{}", getFlvPath());
        log.info("HLS 路径：{}", getHlsPath());
        log.info("FFmpeg 启用：{}", isFFmpegEnabled());
        log.info("HLS 启用：{}", isHlsEnabled());
        log.info("FFmpeg 路径：{}", getFfmpegPath() != null ? getFfmpegPath() : "自动查找");
        log.info("自动关闭：{}", isAutoClose());
        log.info("无人观看关闭时长：{}ms", getNoClientsDuration());
        log.info("===============================");
    }
}
