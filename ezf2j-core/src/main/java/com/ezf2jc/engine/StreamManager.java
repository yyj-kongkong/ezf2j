package com.ezf2jc.engine;


import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.service.MediaService;
import com.ezf2jc.thread.MediaTransfer;
import com.ezf2jc.thread.MediaTransferFlvByFFmpeg;
import com.ezf2jc.thread.MediaTransferFlvByJavacv;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流管理器
 * 管理所有活跃的推流，提供流状态查询、批量控制等功能
 *
 * @author ZJ
 */
@Slf4j
public class StreamManager {

    /**
     * MediaService 实例
     */
    private final MediaService mediaService;

    /**
     * 流状态缓存
     */
    private final Map<String, StreamInfo> streamInfoMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * @param mediaService MediaService 实例
     */
    public StreamManager(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * 获取所有活跃的流
     * @return 流信息列表
     */
    public List<StreamInfo> getAllStreams() {
        List<StreamInfo> streams = new ArrayList<>();

        for (Map.Entry<String, MediaTransfer> entry : MediaService.cameras.entrySet()) {
            MediaTransfer transfer = entry.getValue();
            StreamInfo info = new StreamInfo();
            info.setMediaKey(entry.getKey());
            info.setWcSize(transfer.wcSize);
            info.setHcSize(transfer.hcSize);
            info.setRunning(true);

            if (transfer instanceof MediaTransferFlvByJavacv) {
                MediaTransferFlvByJavacv javacv = (MediaTransferFlvByJavacv) transfer;
                info.setMode("JavaCV");
                info.setRunning(javacv.isRunning());
            } else if (transfer instanceof MediaTransferFlvByFFmpeg) {
                MediaTransferFlvByFFmpeg ffmpeg = (MediaTransferFlvByFFmpeg) transfer;
                info.setMode("FFmpeg");
                info.setRunning(ffmpeg.isRunning());
            }

            streams.add(info);
        }

        return streams;
    }

    /**
     * 获取指定流的信息
     * @param streamUrl 流地址
     * @return 流信息
     */
    public StreamInfo getStreamInfo(String streamUrl) {
        String mediaKey = cn.hutool.crypto.digest.MD5.create().digestHex(streamUrl);
        MediaTransfer transfer = MediaService.cameras.get(mediaKey);
        if (transfer == null)return null;
        if (transfer == null) {
            return null;
        }

        StreamInfo info = new StreamInfo();
        info.setMediaKey(mediaKey);
        info.setUrl(streamUrl);
        info.setWcSize(transfer.wcSize);
        info.setHcSize(transfer.hcSize);
        info.setTotalClients(transfer.wcSize + transfer.hcSize);

        if (transfer instanceof MediaTransferFlvByJavacv) {
            MediaTransferFlvByJavacv javacv = (MediaTransferFlvByJavacv) transfer;
            info.setMode("JavaCV");
            info.setRunning(javacv.isRunning());
            info.setGrabberStatus(javacv.isGrabberStatus());
            info.setRecorderStatus(javacv.isRecorderStatus());
        } else if (transfer instanceof MediaTransferFlvByFFmpeg) {
            MediaTransferFlvByFFmpeg ffmpeg = (MediaTransferFlvByFFmpeg) transfer;
            info.setMode("FFmpeg");
            info.setRunning(ffmpeg.isRunning());
        }

        return info;
    }

    /**
     * 批量关闭所有流
     */
    public void stopAllStreams() {
        Set<String> mediaKeys = new HashSet<>(MediaService.cameras.keySet());

        for (String mediaKey : mediaKeys) {
            try {
                CameraDto cameraDto = new CameraDto();
                cameraDto.setMediaKey(mediaKey);
                mediaService.closeForApi(cameraDto);
                log.info("已关闭流：{}", mediaKey);
            } catch (Exception e) {
                log.error("关闭流失败：{}", mediaKey, e);
            }
        }

        log.info("已关闭所有流，总数：{}", mediaKeys.size());
    }

    /**
     * 批量关闭指定的流
     * @param streamUrls 流地址列表
     */
    public void stopStreams(Collection<String> streamUrls) {
        for (String streamUrl : streamUrls) {
            try {
                stopStream(streamUrl);
            } catch (Exception e) {
                log.error("关闭流失败：{}", streamUrl, e);
            }
        }
    }

    /**
     * 关闭指定的流
     * @param streamUrl 流地址
     */
    public void stopStream(String streamUrl) {
        String mediaKey = cn.hutool.crypto.digest.MD5.create().digestHex(streamUrl);
        CameraDto cameraDto = new CameraDto();
        cameraDto.setUrl(streamUrl);
        cameraDto.setMediaKey(mediaKey);

        mediaService.closeForApi(cameraDto);
        log.info("已关闭流：{}", streamUrl);
    }

    /**
     * 获取活跃流数量
     * @return 活跃流数量
     */
    public int getActiveStreamCount() {
        return MediaService.cameras.size();
    }

    /**
     * 获取总客户端数量
     * @return 总客户端数量
     */
    public int getTotalClientCount() {
        int total = 0;
        for (MediaTransfer transfer : MediaService.cameras.values()) {
            total += transfer.wcSize + transfer.hcSize;
        }
        return total;
    }

    /**
     * 流信息内部类
     */
    public static class StreamInfo {
        private String mediaKey;
        private String url;
        private String mode;
        private boolean running;
        private boolean grabberStatus;
        private boolean recorderStatus;
        private int wcSize;
        private int hcSize;
        private int totalClients;

        public String getStreamId() {
            return streamId;
        }

        public void setStreamId(String streamId) {
            this.streamId = streamId;
        }

        /**
         * Stream ID（6 位随机字符）
         */
        private  String streamId;

        // Getters and Setters
        public String getMediaKey() { return mediaKey; }
        public void setMediaKey(String mediaKey) { this.mediaKey = mediaKey; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }

        public boolean isGrabberStatus() { return grabberStatus; }
        public void setGrabberStatus(boolean grabberStatus) { this.grabberStatus = grabberStatus; }

        public boolean isRecorderStatus() { return recorderStatus; }
        public void setRecorderStatus(boolean recorderStatus) { this.recorderStatus = recorderStatus; }

        public int getWcSize() { return wcSize; }
        public void setWcSize(int wcSize) { this.wcSize = wcSize; }

        public int getHcSize() { return hcSize; }
        public void setHcSize(int hcSize) { this.hcSize = hcSize; }

        public int getTotalClients() { return totalClients; }
        public void setTotalClients(int totalClients) { this.totalClients = totalClients; }

        @Override
        public String toString() {
            return String.format("StreamInfo{key=%s, mode=%s, running=%s, clients=%d}",
                    mediaKey, mode, running, totalClients);
        }
    }
}
