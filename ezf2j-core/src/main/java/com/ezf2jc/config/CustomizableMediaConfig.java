package com.ezf2jc.config;


import com.ezf2jc.service.HlsService;
import com.ezf2jc.service.MediaService;
import com.ezf2jc.thread.MediaTransferHls;
import lombok.extern.slf4j.Slf4j;

/**
 * 可定制的媒体服务器配置
 * 支持链式调用和自定义扩展
 *
 * @author ZJ
 */
@Slf4j
public class CustomizableMediaConfig extends BaseMediaConfig {

    /**
     * 配置回调接口
     */
    public interface ConfigCallback {
        void onApply(BaseMediaConfig config);
    }

    /**
     * 配置回调
     */
    private ConfigCallback callback;

    /**
     * 设置配置回调
     * @param callback 回调函数
     * @return 当前配置对象
     */
    public CustomizableMediaConfig setCallback(ConfigCallback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void apply() {
        if (!validate()) {
            throw new RuntimeException("配置验证失败");
        }

        // 执行自定义回调
        if (callback != null) {
            callback.onApply(this);
        }

        // 应用 MediaService 配置
        MediaConfig mediaConfig = new MediaConfig();
        mediaConfig.setPort(getPort());
        mediaConfig.setHost(getHost());
        mediaConfig.setNetTimeout(getNetTimeout());
        mediaConfig.setReadOrWriteTimeout(getReadOrWriteTimeout());
        mediaConfig.setAutoClose(isAutoClose());
        mediaConfig.setNoClientsDuration(getNoClientsDuration());
        mediaConfig.setEnableFFmpeg(isEnableFFmpeg());
        mediaConfig.setFfmpegPath(getFfmpegPath());
        mediaConfig.setLogLevel(getLogLevel());

        // 应用 HLS 配置
        HlsService.setDefaultPort(getHlsConfig().getPort());
        MediaTransferHls.setHlsHost(getHlsConfig().getHost());

        MediaService.setGlobalConfig(mediaConfig);

        log.info("✅ 自定义配置已应用 - 端口:{}, 地址:{}, FFmpeg:{}, 日志级别:{}",
                getPort(), getHost(), isEnableFFmpeg(), getLogLevel());
    }

    /**
     * 快速配置端口
     * @param port 端口号
     * @return 当前配置对象
     */
    public CustomizableMediaConfig withPort(int port) {
        setPort(port);
        return this;
    }

    /**
     * 快速配置主机地址
     * @param host 主机地址
     * @return 当前配置对象
     */
    public CustomizableMediaConfig withHost(String host) {
        setHost(host);
        return this;
    }

    /**
     * 快速启用 FFmpeg
     * @return 当前配置对象
     */
    public CustomizableMediaConfig withFFmpeg() {
        setEnableFFmpeg(true);
        return this;
    }

    /**
     * 快速配置自动关闭
     * @param autoClose 是否自动关闭
     * @return 当前配置对象
     */
    public CustomizableMediaConfig withAutoClose(boolean autoClose) {
        setAutoClose(autoClose);
        return this;
    }

    /**
     * 快速配置无人观看关闭时长
     * @param duration 时长（毫秒）
     * @return 当前配置对象
     */
    public CustomizableMediaConfig withNoClientsDuration(long duration) {
        setNoClientsDuration(duration);
        return this;
    }

    /**
     * 快速配置 HLS
     * @param host HLS 主机
     * @param port HLS 端口
     * @return 当前配置对象
     */
    public CustomizableMediaConfig withHls(String host, int port) {
        getHlsConfig().setHost(host);
        getHlsConfig().setPort(port);
        return this;
    }

    /**
     * 快速配置日志级别
     * @param level 日志级别（0-4）
     * @return 当前配置对象
     */
    public CustomizableMediaConfig withLogLevel(int level) {
        setLogLevel(level);
        return this;
    }
}
