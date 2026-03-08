package com.ezf2jc.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 媒体服务器基础配置抽象类
 * 所有配置类都应继承此类
 *
 * @author ZJ
 */
@Slf4j
@Data
public abstract class BaseMediaConfig {

    /**
     * 服务器端口，默认 8866
     */
    protected int port = 8866;

    /**
     * 绑定地址，默认 0.0.0.0
     */
    protected String host = "0.0.0.0";

    /**
     * 网络超时时间（毫秒），默认 15 秒
     */
    protected long netTimeout = 15000000;

    /**
     * 读写超时时间（毫秒），默认 15 秒
     */
    protected long readOrWriteTimeout = 15000000;

    /**
     * 是否自动关闭无人观看的流，默认 true
     */
    protected boolean autoClose = true;

    /**
     * 无人观看多久后自动关闭（毫秒），默认 60 秒
     */
    protected long noClientsDuration = 6000000;

    /**
     * 是否启用 FFmpeg，默认 false（使用 JavaCV）
     */
    protected boolean enableFFmpeg = false;

    /**
     * FFmpeg 可执行文件路径（为空则自动查找）
     */
    protected String ffmpegPath;

    /**
     * 日志级别：0=关闭，1=错误，2=警告，3=信息，4=调试
     */
    protected int logLevel = 3;

    /**
     * HLS 相关配置
     */
    protected HlsConfig hlsConfig = new HlsConfig();

    /**
     * 应用此配置到全局
     * 子类可以重写此方法来自定义配置应用逻辑
     */
    public abstract void apply();

    /**
     * 验证配置是否合法
     * @return 是否合法
     */
    public boolean validate() {
        if (port <= 0 || port > 65535) {
            log.error("端口号必须在 1-65535 之间");
            return false;
        }

        if (netTimeout < 1000) {
            log.error("网络超时时间不能小于 1000ms");
            return false;
        }

        if (readOrWriteTimeout < 1000) {
            log.error("读写超时时间不能小于 1000ms");
            return false;
        }

        if (noClientsDuration < 10000) {
            log.error("无人观看自动关闭时间不能小于 10000ms");
            return false;
        }

        if (logLevel < 0 || logLevel > 4) {
            log.error("日志级别必须在 0-4 之间");
            return false;
        }

        if (host == null || host.trim().isEmpty()) {
            log.error("主机地址不能为空");
            return false;
        }

        return true;
    }

    /**
     * HLS 配置内部类
     */
    @Data
    public static class HlsConfig {
        /**
         * HLS 服务器主机地址
         */
        private String host = "localhost";

        /**
         * HLS 服务器端口
         */
        private int port = 8866;

        /**
         * TS 分片时长（秒）
         */
        private int time = 1;

        /**
         * HLS wrap 数量
         */
        private int wrap = 6;

        /**
         * HLS list size
         */
        private int listSize = 1;
    }
}
