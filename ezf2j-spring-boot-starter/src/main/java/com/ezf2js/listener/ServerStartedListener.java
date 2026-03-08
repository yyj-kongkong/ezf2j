package com.ezf2js.listener;

import com.ezf2jc.engine.MediaEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 服务器启动监听器
 * Spring Boot 完全启动完成后触发
 *
 * @author ZJ
 */
@Slf4j
@Component
public class ServerStartedListener {

    /**
     * MediaEngine 实例（可选）
     */
    @Autowired(required = false)
    private MediaEngine mediaEngine;

    /**
     * 监听应用准备就绪事件
     * @param event 应用准备就绪事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (mediaEngine != null) {
            log.info("🎉 ============================================");
            log.info("🎉 媒体服务器已启动并准备就绪");
            log.info("🎉 服务地址：{}:{}",
                    mediaEngine.getConfig().getHost(),
                    mediaEngine.getConfig().getPort());
            log.info("🎉 WebSocket 播放：ws://{}:{}/live?url=<流地址>",
                    mediaEngine.getConfig().getHost(),
                    mediaEngine.getConfig().getPort());
            log.info("🎉 HTTP-FLV 播放：http://{}:{}/live?url=<流地址>",
                    mediaEngine.getConfig().getHost(),
                    mediaEngine.getConfig().getPort());
            log.info("🎉 HLS 播放：http://{}:{}/hls/<mediaKey>/out.m3u8",
                    mediaEngine.getConfig().getHlsConfig().getHost(),
                    mediaEngine.getConfig().getHlsConfig().getPort());
            log.info("🎉 ============================================");
        } else {
            log.warn("⚠️  媒体服务未启用，请检查配置 ezf2j.media.enabled=true");
        }
    }
}
