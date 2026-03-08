package com.ezf2js.config;

import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.config.BaseMediaConfig.HlsConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot 环境下的配置实现
 * 基于 MediaServerProperties 构建配置对象
 *
 * @author ZJ
 */
@Slf4j
public class SpringBootMediaConfig extends BaseMediaConfig {

    /**
     * 构造函数
     * @param properties 配置属性
     */
    public SpringBootMediaConfig(MediaServerProperties properties) {
        // 设置基本配置
        this.setPort(properties.getPort());
        this.setHost(properties.getHost());
        this.setNetTimeout(properties.getNetTimeout());
        this.setReadOrWriteTimeout(properties.getReadOrWriteTimeout());
        this.setAutoClose(properties.getAutoClose());
        this.setNoClientsDuration(properties.getNoClientsDuration());
        this.setEnableFFmpeg(properties.getEnableFFmpeg());
        this.setFfmpegPath(properties.getFfmpegPath());
        this.setLogLevel(properties.getLogLevel());

        // 设置 HLS 配置
        if (properties.getHls() != null) {
            HlsConfig hlsConfig = new HlsConfig();
            hlsConfig.setPort(properties.getHls().getPort());
            hlsConfig.setHost(properties.getHls().getHost());
            this.setHlsConfig(hlsConfig);
        }

        log.debug("SpringBootMediaConfig 初始化完成：{}", this);
    }

    @Override
    public void apply() {
        log.info("应用 Spring Boot 配置...");

        // 设置 FFmpeg 路径
        if (this.getFfmpegPath() != null && !this.getFfmpegPath().isEmpty()) {
            System.setProperty("ffmpeg.path", this.getFfmpegPath());
        }

        // 设置日志级别

        log.info("✅ Spring Boot 配置应用完成 - 端口:{}, 主机:{}, 自动关闭:{}",
                this.getPort(), this.getHost(), this.isAutoClose());
    }

    @Override
    public boolean validate() {
        // 端口验证
        if ( this.getPort() <= 0 || this.getPort() > 65535) {
            log.error("无效的端口号：{}", this.getPort());
            return false;
        }

        // 主机地址验证
        if (this.getHost() == null || this.getHost().trim().isEmpty()) {
            log.error("无效的主机地址：{}", this.getHost());
            return false;
        }

        // 超时时间验证
        try {
            if (getNetTimeout() < 1000000) {
                log.error("网络超时时间不能小于 1 秒：{}", this.getNetTimeout());
                return false;
            }
        } catch (NumberFormatException e) {
            log.error("网络超时时间格式错误：{}", this.getNetTimeout());
            return false;
        }

        // 无人观看关闭时间验证
        if (this.getNoClientsDuration() < 10000) {
            log.error("无人观看关闭时间不能小于 10 秒：{}", this.getNoClientsDuration());
            return false;
        }

        log.debug("配置验证通过");
        return true;
    }
}
