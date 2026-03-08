package com.ezf2jc.config;

import lombok.Data;

/**
 * MediaService 专用配置类
 *
 * @author ZJ
 */
@Data
public class MediaConfig {

    /**
     * 服务器端口，默认 8866
     */
    private int port = 8866;

    /**
     * 绑定地址，默认 0.0.0.0
     */
    private String host = "0.0.0.0";

    /**
     * 网络超时时间（毫秒），默认 15 秒
     */
    private long netTimeout = 15000000;

    /**
     * 读写超时时间（毫秒），默认 15 秒
     */
    private long readOrWriteTimeout = 15000000;

    /**
     * 是否自动关闭无人观看的流，默认 true
     */
    private boolean autoClose = true;

    /**
     * 无人观看多久后自动关闭（毫秒），默认 60 秒
     */
    private long noClientsDuration = 6000000;

    /**
     * 是否启用 FFmpeg，默认 false（使用 JavaCV）
     */
    private boolean enableFFmpeg = false;

    /**
     * FFmpeg 可执行文件路径（为空则自动查找）
     */
    private String ffmpegPath;

    /**
     * 日志级别：0=关闭，1=错误，2=警告，3=信息，4=调试
     */
    private int logLevel = 3;

    /**
     * 构建默认配置
     * @return 配置对象
     */
    public static MediaConfig defaultConfig() {
        return new MediaConfig();
    }

    /**
     * 快速配置（仅端口）
     * @param port 端口号
     * @return 配置对象
     */
    public static MediaConfig quickConfig(int port) {
        MediaConfig config = new MediaConfig();
        config.setPort(port);
        return config;
    }

    /**
     * 链式配置端口
     * @param port 端口号
     * @return 当前配置对象
     */
    public MediaConfig withPort(int port) {
        setPort(port);
        return this;
    }

    /**
     * 链式配置主机地址
     * @param host 主机地址
     * @return 当前配置对象
     */
    public MediaConfig withHost(String host) {
        setHost(host);
        return this;
    }

    /**
     * 链式配置 FFmpeg
     * @param enable 是否启用
     * @return 当前配置对象
     */
    public MediaConfig withFFmpeg(boolean enable) {
        setEnableFFmpeg(enable);
        return this;
    }

    /**
     * 链式配置自动关闭
     * @param autoClose 是否自动关闭
     * @return 当前配置对象
     */
    public MediaConfig withAutoClose(boolean autoClose) {
        setAutoClose(autoClose);
        return this;
    }

    /**
     * 链式配置无人观看关闭时长
     * @param duration 时长（毫秒）
     * @return 当前配置对象
     */
    public MediaConfig withNoClientsDuration(long duration) {
        setNoClientsDuration(duration);
        return this;
    }
}
