package com.ezf2js.listener;

import com.ezf2jc.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 流事件监听器
 * 定时监控流状态和统计信息
 *
 * @author ZJ
 */
@Slf4j
@Component
public class StreamEventListener {

    /**
     * MediaService 实例（可选）
     */
    @Autowired(required = false)
    private MediaService mediaService;

    /**
     * 上次检查的流数量
     */
    private final AtomicLong lastStreamCount = new AtomicLong(0);

    /**
     * 定时任务：每 5 秒检查一次流状态
     */
    @Scheduled(fixedRate = 5000)
    public void checkStreamStatus() {
        if (mediaService == null) {
            return;
        }

        try {
            int currentCount = mediaService.getActiveStreamCount();
            long lastCount = lastStreamCount.get();

            if (currentCount != lastCount) {
                if (currentCount > lastCount) {
                    log.info("📺 新增 {} 个活跃流，当前总数：{}",
                            currentCount - lastCount, currentCount);
                } else {
                    log.info("📺 减少 {} 个活跃流，当前总数：{}",
                            lastCount - currentCount, currentCount);
                }
                lastStreamCount.set(currentCount);
            }

            // 记录详细日志（调试用）
            if (log.isDebugEnabled()) {
                String[] streams = mediaService.getActiveStreams();
                if (streams != null && streams.length > 0) {
                    log.debug("当前活跃流列表：");
                    for (String stream : streams) {
                        log.debug("  - {}", stream);
                    }
                }
            }

        } catch (Exception e) {
            log.error("检查流状态失败", e);
        }
    }

    /**
     * 定时任务：每分钟打印统计信息
     */
    @Scheduled(fixedRate = 60000)
    public void printStatistics() {
        if (mediaService == null) {
            return;
        }

        try {
            int activeCount = mediaService.getActiveStreamCount();

            if (activeCount > 0) {
                log.info("📊 流统计 - 活跃流数量：{}, 时间：{}",
                        activeCount,
                        java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        } catch (Exception e) {
            log.error("打印统计信息失败", e);
        }
    }
}
