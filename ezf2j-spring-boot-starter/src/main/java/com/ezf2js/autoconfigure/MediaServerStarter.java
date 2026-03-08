package com.ezf2js.autoconfigure;

import com.ezf2jc.engine.MediaEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 媒体服务器启动器
 * Spring Boot 启动完成后自动运行
 *
 * @author ZJ
 */
@Slf4j
@Component
public class MediaServerStarter implements ApplicationRunner {

    /**
     * MediaEngine 实例（由 Spring 注入）
     */
    @Autowired(required = false)
    private MediaEngine mediaEngine;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (mediaEngine != null) {
            log.info("🚀 媒体服务器已启动，可以开始推流了");
            log.info("WebSocket 地址：ws://{}:{}/live?url=<流地址>",
                    mediaEngine.getConfig().getHost(),
                    mediaEngine.getConfig().getPort());
            log.info("HTTP-FLV 地址：http://{}:{}/live?url=<流地址>",
                    mediaEngine.getConfig().getHost(),
                    mediaEngine.getConfig().getPort());
        } else {
            log.warn("MediaEngine 未注入，请检查配置是否启用（ezf2j.media.enabled=true）");
        }
    }
}
