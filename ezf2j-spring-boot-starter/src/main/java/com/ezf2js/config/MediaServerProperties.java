package com.ezf2js.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 媒体服务器配置属性
 * 从 application.yml 或 application.properties 加载配置
 *
 * @author ZJ
 */
@Data
@ConfigurationProperties(prefix = "ezf2j.media")
public class MediaServerProperties {

    /**
     * 是否启用媒体服务（默认 true）
     */
    private Boolean enabled = true;

    /**
     * 监听端口（默认 8866）
     */
    private Integer port = 8866;

    /**
     * 监听主机地址（默认 0.0.0.0）
     */
    private String host = "0.0.0.0";

    /**
     * 网络超时时间（微秒，默认 15000000=15 秒）
     */
    private int netTimeout = 15000000;

    /**
     * 读写超时时间（微秒，默认 15000000=15 秒）
     */
    private int readOrWriteTimeout = Math.toIntExact(15000000L);

    /**
     * 无人观看是否自动关闭（默认 true）
     */
    private Boolean autoClose = true;

    /**
     * 无人观看关闭时长（毫秒，默认 60000=60 秒）
     */
    private Long noClientsDuration = 60000L;

    /**
     * 是否启用 FFmpeg（默认 false=JavaCV）
     */
    private Boolean enableFFmpeg = false;

    /**
     * FFmpeg 路径（可选，默认自动查找）
     */
    private String ffmpegPath;

    /**
     * 日志级别（默认 INFO）
     */
    private int logLevel = 3;

    /**
     * HLS 配置
     */
    private HlsConfig hls = new HlsConfig();

    /**
     * HLS 配置内部类
     */
    @Data
    public static class HlsConfig {
        /**
         * HLS 监听端口（默认 8866）
         */
        private Integer port = 8866;

        /**
         * HLS 主机地址（默认 localhost）
         */
        private String host = "localhost";
    }
}
