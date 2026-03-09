package com.ezf2jc.config;

import com.ezf2jc.service.HlsService;
import com.ezf2jc.service.MediaService;
import com.ezf2jc.thread.MediaTransferHls;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认媒体服务器配置
 * 使用默认值，可直接使用或作为父类参考
 *
 * @author ZJ
 */
@Slf4j
public class DefaultMediaConfig extends BaseMediaConfig {

    @Override
    public void apply() {
        if (!validate()) {
            throw new RuntimeException("配置验证失败");
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

        MediaService.setGlobalConfig(mediaConfig);

        // 应用 HLS 配置
        HlsService.setDefaultPort(getHlsConfig().getPort());
        MediaTransferHls.setHlsHost(getHlsConfig().getHost());

        log.info("✅ 默认配置已应用 - 端口:{}, 地址:{}, 自动关闭:{}, 关闭时长:{}ms",
                getPort(), getHost(), isAutoClose(), getNoClientsDuration());
    }
}
