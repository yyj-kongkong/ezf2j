package com.ezf2js.engine;

import cn.hutool.crypto.digest.MD5;
import com.ez2fj.EZF2JEngine;
import com.ez2fj.init.InitConfig;
import com.ez2fj.model.Camera;
import com.ez2fj.server.MediaServer;
import com.ez2fj.service.CameraService;
import com.ez2fj.service.HlsService;
import com.ez2fj.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class SpringMediaEngine {

    private MediaService mediaService;

    private HlsService hlsService;

    private CameraService cameraService;

    private MediaServer mediaServer;

    private InitConfig config;

    public void setMediaService(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    public void setHlsService(HlsService hlsService) {
        this.hlsService = hlsService;
    }

    public void setCameraService(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    public void setMediaServer(MediaServer mediaServer) {
        this.mediaServer = mediaServer;
    }

    public void setConfig(InitConfig config) {
        this.config = config;
    }

    public String addCamera(String streamUrl) {
        return addCamera(streamUrl, false);
    }

    public String addCamera(String streamUrl, boolean useFFmpeg) {
        Camera camera = buildCamera(streamUrl, useFFmpeg);
        String mediaKey = cameraService.addCamera(camera);
        log.info("✅ 添加相机成功：{}, mediaKey: {}", streamUrl, mediaKey);
        return mediaKey;
    }

    public String addCamera(String streamUrl, boolean useFFmpeg, boolean autoClose, long noClientsDuration) {
        Camera camera = buildCamera(streamUrl, useFFmpeg);
        camera.setAutoClose(autoClose);
        camera.setNoClientsDuration(noClientsDuration);
        
        String mediaKey = cameraService.addCamera(camera);
        log.info("✅ 添加相机成功：{} (自动关闭:{}ms)", streamUrl, noClientsDuration);
        return mediaKey;
    }

    public boolean removeCamera(String streamUrl) {
        String mediaKey = MD5.create().digestHex(streamUrl);
        boolean removed = cameraService.removeCamera(mediaKey);
        if (removed) {
            log.info("🛑 已移除相机：{}", streamUrl);
        } else {
            log.warn("⚠️ 相机不存在：{}", streamUrl);
        }
        return removed;
    }

    public boolean removeCameraByMediaKey(String mediaKey) {
        boolean removed = cameraService.removeCamera(mediaKey);
        if (removed) {
            log.info("🛑 已移除相机：{}", mediaKey);
        }
        return removed;
    }

    public Camera updateCamera(String streamUrl, boolean useFFmpeg) {
        String mediaKey = MD5.create().digestHex(streamUrl);
        Camera camera = cameraService.getCamera(mediaKey);
        if (camera == null) {
            log.error("❌ 相机不存在：{}", streamUrl);
            return null;
        }
        
        camera.setEnabledFFmpeg(useFFmpeg);
        Camera updated = cameraService.updateCamera(camera);
        log.info("🔄 已更新相机：{}", streamUrl);
        return updated;
    }

    public Camera getCamera(String streamUrl) {
        String mediaKey = MD5.create().digestHex(streamUrl);
        return cameraService.getCamera(mediaKey);
    }

    public Camera getCameraByMediaKey(String mediaKey) {
        return cameraService.getCamera(mediaKey);
    }

    public List<Camera> getAllCameras() {
        Collection<Camera> cameras = cameraService.getAllCameras();
        return new ArrayList<>(cameras);
    }

    public int getCameraCount() {
        return cameraService.getCameraCount();
    }

    public boolean hasCamera(String streamUrl) {
        String mediaKey = MD5.create().digestHex(streamUrl);
        return cameraService.containsCamera(mediaKey);
    }

    public String startStream(String streamUrl) {
        return addCamera(streamUrl, false);
    }

    public String startStream(String streamUrl, boolean useFFmpeg) {
        return addCamera(streamUrl, useFFmpeg);
    }

    public void stopStream(String streamUrl) {
        removeCamera(streamUrl);
    }

    public String getWsPlayUrl(String streamUrl) {
        return String.format("ws://localhost:%d%s?url=%s", 
                config.getPort(), config.getFlvPath(), streamUrl);
    }

    public String getHttpPlayUrl(String streamUrl) {
        return String.format("http://localhost:%d%s?url=%s", 
                config.getPort(), config.getFlvPath(), streamUrl);
    }

    public String getHlsPlayUrl(String streamUrl) {
        String mediaKey = MD5.create().digestHex(streamUrl);
        return String.format("http://localhost:%d%s/%s/out.m3u8", 
                config.getPort(), config.getHlsPath(), mediaKey);
    }

    public void shutdown() {
        log.info("正在停止 EZF2J 服务...");
        EZF2JEngine.getInstance().shutdown();
        log.info("✅ EZF2J 服务已停止");
    }

    public MediaService getMediaService() {
        return mediaService;
    }

    public HlsService getHlsService() {
        return hlsService;
    }

    public CameraService getCameraService() {
        return cameraService;
    }

    public MediaServer getMediaServer() {
        return mediaServer;
    }

    public InitConfig getConfig() {
        return config;
    }

    private Camera buildCamera(String streamUrl, boolean useFFmpeg) {
        Camera camera = new Camera();
        camera.setUrl(streamUrl);
        camera.setEnabledFFmpeg(useFFmpeg);
        camera.setEnabledFlv(true);
        camera.setEnabledHls(config.isEnableHls());
        camera.setAutoClose(config.isAutoClose());
        camera.setNoClientsDuration(config.getNoClientsDuration());
        camera.setNetTimeout(config.getNetTimeout());
        camera.setReadOrWriteTimeout(config.getReadOrWriteTimeout());
        
        String mediaKey = MD5.create().digestHex(streamUrl);
        camera.setMediaKey(mediaKey);
        
        if (isLocalFile(streamUrl)) {
            camera.setType(1);
        }
        
        return camera;
    }

    private boolean isLocalFile(String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) {
            return false;
        }
        String[] split = streamUrl.trim().split("\\:");
        return split.length > 0 && split[0].length() <= 1;
    }
}
