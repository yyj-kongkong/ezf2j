package com.ez2fj.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.crypto.digest.MD5;
import com.ez2fj.model.Camera;
import com.ez2fj.thread.MediaTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Camera 相机管理服务类
 * 提供相机的增删改查功能，支持自动推流、定时清理无人观看的流
 * @author ZJ
 */
public class CameraService {
    private static volatile CameraService cameraService;

    public static CameraService getInstance() {
        if (cameraService == null) {
            synchronized (HlsService.class) {
                if (cameraService == null) {
                    cameraService = new CameraService();
                }
            }
        }
        return cameraService;
    }
    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);
    
    /**
     * 使用 ConcurrentHashMap 保证线程安全
     * key: mediaKey (相机唯一标识)
     * value: Camera 对象
     */
    private final Map<String, Camera> cameraMap = new ConcurrentHashMap<>();
    
    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * 是否启动定时清理任务
     */
    private boolean cleanupTaskStarted = false;
    
    /**
     * 默认清理间隔（秒）
     */
    private static final int DEFAULT_CLEANUP_INTERVAL = 30;
    
    /**
     * 构造方法，启动定时清理任务
     */
    private CameraService() {
        startCleanupTask();
    }


    /**
     * 添加相机，自动推流
     * @param camera 相机对象
     * @return 添加成功的相机 mediaKey
     */
    public String addCamera(Camera camera) {
        if (camera == null) {
            throw new IllegalArgumentException("Camera 对象不能为空");
        }
        
        // 如果没有设置 mediaKey，则根据 URL 生成 MD5
        if (camera.getMediaKey() == null || camera.getMediaKey().isEmpty()) {
            String mediaKey = MD5.create().digestHex(camera.getUrl());
            camera.setMediaKey(mediaKey);
            logger.info("为相机生成 mediaKey: {}", mediaKey);
        }
        
        // 如果已存在则抛出异常
        if (cameraMap.containsKey(camera.getMediaKey())) {
            throw new IllegalStateException("该 mediaKey 的相机已存在：" + camera.getMediaKey());
        }
        
        cameraMap.put(camera.getMediaKey(), camera);
        logger.info("成功添加相机：{}, URL: {}", camera.getMediaKey(), camera.getUrl());
        
        // 自动推流
        autoStartStream(camera);
        
        return camera.getMediaKey();
    }
    
    /**
     * 删除相机，关闭流
     * @param mediaKey 相机唯一标识
     * @return 删除是否成功
     */
    public boolean removeCamera(String mediaKey) {
        if (mediaKey == null || mediaKey.isEmpty()) {
            throw new IllegalArgumentException("mediaKey 不能为空");
        }
        
        Camera camera = cameraMap.get(mediaKey);
        if (camera != null) {
            // 先关闭流
            stopStream(camera);
        }
        
        Camera removed = cameraMap.remove(mediaKey);
        if (removed != null) {
            logger.info("成功删除相机：{}", mediaKey);
            return true;
        } else {
            logger.warn("未找到要删除的相机：{}", mediaKey);
            return false;
        }
    }
    
    /**
     * 更新相机信息，重新推流
     * @param camera 新的相机对象
     * @return 更新后的相机对象
     */
    public Camera updateCamera(Camera camera) {
        if (camera == null) {
            throw new IllegalArgumentException("Camera 对象不能为空");
        }
        
        if (camera.getMediaKey() == null || camera.getMediaKey().isEmpty()) {
            throw new IllegalArgumentException("mediaKey 不能为空");
        }
        
        if (!cameraMap.containsKey(camera.getMediaKey())) {
            throw new IllegalStateException("该 mediaKey 的相机不存在：" + camera.getMediaKey());
        }
        
        // 先关闭旧流
        Camera oldCamera = cameraMap.get(camera.getMediaKey());
        stopStream(oldCamera);
        
        // 再更新并启动新流
        cameraMap.put(camera.getMediaKey(), camera);
        logger.info("成功更新相机：{}", camera.getMediaKey());
        
        // 重新推流
        autoStartStream(camera);
        
        return camera;
    }
    
    /**
     * 根据 mediaKey 查询相机
     * @param mediaKey 相机唯一标识
     * @return 相机对象，不存在返回 null
     */
    public Camera getCamera(String mediaKey) {
        if (mediaKey == null || mediaKey.isEmpty()) {
            return null;
        }
        return cameraMap.get(mediaKey);
    }
    
    /**
     * 查询所有相机
     * @return 所有相机的集合
     */
    public Collection<Camera> getAllCameras() {
        return cameraMap.values();
    }
    
    /**
     * 检查相机是否存在
     * @param mediaKey 相机唯一标识
     * @return 是否存在
     */
    public boolean containsCamera(String mediaKey) {
        return cameraMap.containsKey(mediaKey);
    }
    
    /**
     * 获取相机数量
     * @return 相机总数
     */
    public int getCameraCount() {
        return cameraMap.size();
    }
    
    /**
     * 清空所有相机，关闭所有流
     */
    public void clearAllCameras() {
        logger.info("开始清空所有 {} 个相机配置", cameraMap.size());
        
        // 关闭所有流
        for (Camera camera : cameraMap.values()) {
            stopStream(camera);
        }
        
        cameraMap.clear();
        logger.info("已清空所有相机配置");
    }
    
    /**
     * 停止定时清理任务
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("CameraService 已关闭");
    }
    
    /**
     * 启动定时清理任务，清理无人观看的相机
     */
    private void startCleanupTask() {
        if (cleanupTaskStarted) {
            return;
        }
        
        cleanupTaskStarted = true;
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupInactiveCameras();
            } catch (Exception e) {
                logger.error("清理任务执行失败", e);
            }
        }, DEFAULT_CLEANUP_INTERVAL, DEFAULT_CLEANUP_INTERVAL, TimeUnit.SECONDS);
        
        logger.info("已启动定时清理任务，间隔：{} 秒", DEFAULT_CLEANUP_INTERVAL);
    }
    
    /**
     * 清理无人观看的相机并关闭流
     */
    private void cleanupInactiveCameras() {
        int cleanedCount = 0;
        
        for (Camera camera : cameraMap.values()) {
            if (camera.isAutoClose() && shouldRemoveCamera(camera)) {
                logger.info("检测到无人观看的相机，准备关闭：{}, 持续时间：{}ms", 
                    camera.getMediaKey(), camera.getNoClientsDuration());
                
                // 关闭流
                stopStream(camera);
                
                // 从缓存中移除
                cameraMap.remove(camera.getMediaKey());
                cleanedCount++;
                
                logger.info("已关闭并移除无人观看的相机：{}", camera.getMediaKey());
            }
        }
        
        if (cleanedCount > 0) {
            logger.info("本次清理共关闭 {} 个无人观看的相机", cleanedCount);
        }
    }
    
    /**
     * 判断相机是否应该被移除（无人观看且超过持续时间）
     * @param camera 相机对象
     * @return 是否应该移除
     */
    private boolean shouldRemoveCamera(Camera camera) {
        MediaTransfer mediaTransfer = MediaService.cameras.get(camera.getMediaKey());
        if (mediaTransfer == null) {
            return false;
        }
        
        // 计算总客户端数
        int totalClients = mediaTransfer.httpClients.size() + mediaTransfer.wsClients.size();
        
        // 如果无人观看且超过设定时间
        if (totalClients == 0 && mediaTransfer.noClient * 1000 >= camera.getNoClientsDuration()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 自动推流
     * @param camera 相机对象
     */
    private void autoStartStream(Camera camera) {
        ThreadUtil.execute(() -> {
            try {
                MediaService mediaService = new MediaService();
                
                // 根据配置决定开启 FLV 或 HLS
                if (camera.isEnabledFlv()) {
                    logger.info("开始为相机自动推流 (FLV): {}", camera.getMediaKey());
                    // 这里调用 MediaService 的 API 推流方法
                    mediaService.playForApi(camera);
                }
                
                if (camera.isEnabledHls()) {
                    logger.info("开始为相机自动推流 (HLS): {}", camera.getMediaKey());
                    HlsService hlsService = HlsService.getInstance();
                    hlsService.startConvertToHls(camera);
                }
                
                logger.info("相机推流已启动：{}", camera.getMediaKey());
            } catch (Exception e) {
                logger.error("相机自动推流失败：{}", camera.getMediaKey(), e);
            }
        });
    }
    
    /**
     * 关闭流
     * @param camera 相机对象
     */
    private void stopStream(Camera camera) {
        try {
            // 关闭 FLV 流
            if (camera.isEnabledFlv()) {
                MediaService mediaService = new MediaService();
                mediaService.closeForApi(camera);
                logger.info("已关闭 FLV 流：{}", camera.getMediaKey());
            }
            
            // 关闭 HLS 流
            if (camera.isEnabledHls()) {
                HlsService hlsService = HlsService.getInstance();
                hlsService.closeConvertToHls(camera);
                logger.info("已关闭 HLS 流：{}", camera.getMediaKey());
            }
        } catch (Exception e) {
            logger.error("关闭流失败：{}", camera.getMediaKey(), e);
        }
    }
    
    /**
     * 生成唯一的 mediaKey（备用方法）
     * @return UUID 格式的 mediaKey
     */
    @SuppressWarnings("unused")
    private String generateMediaKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
