package com.ezf2jc.engine;

import lombok.Data;

/**
 * 引擎配置
 * 用于配置 MediaEngine 的行为
 *
 * @author ZJ
 */
@Data
public class EngineConfig {

    /**
     * 是否启用自动关流，默认 true
     */
    private boolean autoClose = true;

    /**
     * 无人观看自动关闭时长（毫秒），默认 60000ms
     */
    private long noClientsDuration = 60000;

    /**
     * 默认转码方式，false=JavaCV, true=FFmpeg
     */
    private boolean defaultUseFFmpeg = false;

    /**
     * 是否启用流状态监控，默认 true
     */
    private boolean enableStreamMonitor = true;

    /**
     * 流状态监控间隔（毫秒），默认 5000ms
     */
    private long monitorInterval = 5000;

    /**
     * 最大并发流数量，默认 100
     */
    private int maxConcurrentStreams = 100;

    /**
     * 是否记录详细日志，默认 false
     */
    private boolean detailedLogging = false;

    /**
     * 构建默认配置
     * @return 配置对象
     */
    public static EngineConfig defaultConfig() {
        return new EngineConfig();
    }

    /**
     * 快速配置（仅自动关闭时长）
     * @param duration 时长（毫秒）
     * @return 配置对象
     */
    public static EngineConfig quickConfig(long duration) {
        EngineConfig config = new EngineConfig();
        config.setNoClientsDuration(duration);
        return config;
    }
}
