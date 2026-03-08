package com.ezf2jc.service;


import com.ezf2jc.common.StreamStatus;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.dto.StreamIdManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 流生命周期服务
 * 管理流的完整生命周期（创建、运行、停止）
 *
 * @author ZJ
 */
@Slf4j
public class StreamLifecycleService {

    /**
     * 流状态记录
     */
    private final Map<String, StreamRecord> streamRecords = new ConcurrentHashMap<>();

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(5);

    /**
     * MediaService 实例
     */
    private final MediaService mediaService;

    /**
     * 构造函数
     */
    public StreamLifecycleService() {
        this.mediaService = new MediaService();
        startMonitor();
    }

    /**
     * 启动监控任务
     */
    private void startMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorStreams();
            } catch (Exception e) {
                log.error("流监控异常", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * 监控所有流
     */
    private void monitorStreams() {
        long now = System.currentTimeMillis();

        for (Map.Entry<String, StreamRecord> entry : streamRecords.entrySet()) {
            String mediaKey = entry.getKey();
            StreamRecord record = entry.getValue();

            // 检查流状态
            boolean exists = mediaService.hasStream(mediaKey);

            if (!exists && record.getStatus() != StreamStatus.STOPPED) {
                // 流不存在但状态不是已停止，标记为错误
                record.setStatus(StreamStatus.ERROR);
                log.warn("流异常消失：{}", mediaKey);
            } else if (exists) {
                // 更新最后活跃时间
                record.setLastActiveTime(now);
            }
        }
    }

    /**
     * 创建流
     * @param cameraDto 相机 DTO
     * @return 是否成功
     */
    public String createStream(CameraDto cameraDto) {
        String mediaKey = cameraDto.getMediaKey();

        if (streamRecords.containsKey(mediaKey)) {
            log.warn("流已存在：{}", mediaKey);
            StreamIdManager streamIdManager = new StreamIdManager();
            return StreamIdManager.getStreamIdByMediaKey(mediaKey);
        }

        // 创建记录
        StreamRecord record = new StreamRecord();
        record.setMediaKey(mediaKey);
        record.setCameraDto(cameraDto);
        record.setStatus(StreamStatus.INITIALIZING);
        record.setCreateTime(System.currentTimeMillis());

        streamRecords.put(mediaKey, record);

        // 启动流
        String success = mediaService.playForApi(cameraDto);

        if (success!=null) {
            record.setStatus(StreamStatus.RUNNING);
            log.info("流创建成功：{}", mediaKey);
        } else {
            record.setStatus(StreamStatus.ERROR);
            log.error("流创建失败：{}", mediaKey);
        }

        return success;
    }

    /**
     * 停止流
     * @param mediaKey 媒体 Key
     */
    public void stopStream(String mediaKey) {
        StreamRecord record = streamRecords.get(mediaKey);
        if (record == null) {
            log.warn("流不存在：{}", mediaKey);
            return;
        }

        CameraDto cameraDto = record.getCameraDto();
        cameraDto.setMediaKey(mediaKey);

        mediaService.closeForApi(cameraDto);
        record.setStatus(StreamStatus.STOPPED);
        record.setStopTime(System.currentTimeMillis());

        log.info("流已停止：{}", mediaKey);
    }

    /**
     * 获取流状态
     * @param mediaKey 媒体 Key
     * @return 流状态
     */
    public StreamStatus getStreamStatus(String mediaKey) {
        StreamRecord record = streamRecords.get(mediaKey);
        if (record == null) {
            return null;
        }
        return record.getStatus();
    }

    /**
     * 获取流记录
     * @param mediaKey 媒体 Key
     * @return 流记录
     */
    public StreamRecord getStreamRecord(String mediaKey) {
        return streamRecords.get(mediaKey);
    }

    /**
     * 流记录内部类
     */
    public static class StreamRecord {
        private String mediaKey;
        private CameraDto cameraDto;
        private StreamStatus status;
        private long createTime;
        private long stopTime;
        private long lastActiveTime;

        // Getters and Setters
        public String getMediaKey() { return mediaKey; }
        public void setMediaKey(String mediaKey) { this.mediaKey = mediaKey; }

        public CameraDto getCameraDto() { return cameraDto; }
        public void setCameraDto(CameraDto cameraDto) { this.cameraDto = cameraDto; }

        public StreamStatus getStatus() { return status; }
        public void setStatus(StreamStatus status) { this.status = status; }

        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }

        public long getStopTime() { return stopTime; }
        public void setStopTime(long stopTime) { this.stopTime = stopTime; }

        public long getLastActiveTime() { return lastActiveTime; }
        public void setLastActiveTime(long lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    }
}
