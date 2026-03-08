package com.ezf2jc.thread;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 媒体监听线程
 * 监控所有转码线程的客户端连接状态
 *
 * @author ZJ
 */
@Slf4j
public class MediaListenThread implements Runnable {

    /**
     * 线程监控 Map
     */
    private static final ConcurrentMap<MediaTransfer, Integer> threadMap = new ConcurrentHashMap<>();

    /**
     * 添加线程到监控
     */
    public static void putThread(MediaTransfer mediaTransfer) {
        log.debug("添加线程到监控：{}", mediaTransfer.getClass().getSimpleName());
        threadMap.put(mediaTransfer, 1);
    }

    /**
     * 从监控移除线程
     */
    public static void removeThread(MediaTransfer mediaTransfer) {
        log.debug("从监控移除线程：{}", mediaTransfer.getClass().getSimpleName());
        threadMap.remove(mediaTransfer);
    }

    /**
     * 获取监控的线程数量
     */
    public static int getMonitorCount() {
        return threadMap.size();
    }

    @Override
    public void run() {
        log.info("启动播放线程的总监视线程......");

        while (true) {
            try {
                // 每秒检查一次所有被监控的线程
                threadMap.forEach((thread, value) -> {
                    try {
                        thread.hasClient();
                    } catch (Exception e) {
                        log.error("检查客户端状态失败", e);
                    }
                });

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("监听线程被中断", e);
                break;
            } catch (Exception e) {
                log.error("监听线程异常", e);
            }
        }
    }
}
