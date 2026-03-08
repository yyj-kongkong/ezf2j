package com.ezf2js.listener;

import com.ezf2jc.loader.MediaServerLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 服务器关闭监听器
 * Spring 容器关闭时优雅停止媒体服务
 *
 * @author ZJ
 */
@Slf4j
@Component
public class ServerShutdownListener {

    /**
     * 监听上下文关闭事件
     * @param event 上下文关闭事件
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed(ContextClosedEvent event) {
        log.info("🛑 正在关闭媒体服务器...");

        try {
            if (MediaServerLoader.isRunning()) {
                // 停止所有流
                MediaServerLoader.stop();
                log.info("✅ 媒体服务器已优雅关闭");
            } else {
                log.info("ℹ️  媒体服务器未运行，无需关闭");
            }
        } catch (Exception e) {
            log.error("❌ 关闭媒体服务器失败", e);
        }
    }
}
