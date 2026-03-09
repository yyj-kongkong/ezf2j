package com.ez2fj.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class MediaListenThread implements Runnable {
    
    private static ConcurrentMap<MediaTransfer, Integer> threadMap = new ConcurrentHashMap<>();
    
    public static void putThread(MediaTransfer mediaTransfer) {
        log.info("线程{}加入到监视", mediaTransfer);
        threadMap.put(mediaTransfer, 1);
    }
    
    public static void removeThread(MediaTransfer mediaTransfer) {
        log.info("线程{}退出监视", mediaTransfer);
        threadMap.remove(mediaTransfer);
    }
    
    @Override
    public void run() {
        log.info("启动播放线程的总监视线程......");
        while (true) {
            threadMap.forEach((thread, value) -> thread.hasClient());
            try {
                Thread.sleep(1000);
            } catch ( InterruptedException e ) {
            
            }
        }
    }
    
}
