package com.ezf2jc.service;


import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.crypto.digest.MD5;
import com.ezf2jc.common.ClientType;
import com.ezf2jc.config.MediaConfig;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.dto.StreamIdManager;
import com.ezf2jc.thread.MediaTransfer;
import com.ezf2jc.thread.MediaTransferFlvByFFmpeg;
import com.ezf2jc.thread.MediaTransferFlvByJavacv;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体服务
 * 管理所有媒体流的转换和分发
 *
 * @author ZJ
 */
@Slf4j
public class MediaService {

    /**
     * 全局配置
     */
    private static MediaConfig globalConfig = null;

    /**
     * 缓存流转换线程
     */
    public static ConcurrentHashMap<String, MediaTransfer> cameras = new ConcurrentHashMap<>();

    /**
     * 设置全局配置
     * @param config 配置对象
     */
    public static void setGlobalConfig(MediaConfig config) {
        globalConfig = config;
    }

    /**
     * 获取全局配置
     * @return 配置对象
     */
    public static MediaConfig getGlobalConfig() {
        return globalConfig;
    }

    /**
     * HTTP-FLV 播放
     * @param cameraDto 相机 DTO
     * @param ctx 通道上下文
     */
    public void playForHttp(CameraDto cameraDto, ChannelHandlerContext ctx) {

        if (globalConfig != null) {
            applyGlobalConfig(cameraDto);
        }

        if (cameras.containsKey(cameraDto.getMediaKey())) {
            MediaTransfer mediaConvert = cameras.get(cameraDto.getMediaKey());

            if(mediaConvert instanceof MediaTransferFlvByJavacv) {
                MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;

                // 如果当前使用 JavaCV，但要求用 FFmpeg，则重新拉流
                if(cameraDto.isEnabledFFmpeg()) {
                    log.info("切换到 FFmpeg 模式");
                    mediaTransferFlvByJavacv.setRunning(false);
                    cameras.remove(cameraDto.getMediaKey());
                    this.playForHttp(cameraDto, ctx);
                } else {
                    mediaTransferFlvByJavacv.addClient(ctx, ClientType.HTTP);
                }

            } else if (mediaConvert instanceof MediaTransferFlvByFFmpeg) {
                MediaTransferFlvByFFmpeg mediaTransferFlvByFFmpeg = (MediaTransferFlvByFFmpeg) mediaConvert;

                // 如果当前使用 FFmpeg，但要求用 JavaCV，则关闭再重新拉流
                if(!cameraDto.isEnabledFFmpeg()) {
                    log.info("切换到 JavaCV 模式");
                    mediaTransferFlvByFFmpeg.stopFFmpeg();
                    cameras.remove(cameraDto.getMediaKey());
                    this.playForHttp(cameraDto, ctx);
                } else {
                    mediaTransferFlvByFFmpeg.addClient(ctx, ClientType.HTTP);
                }
            }

        } else {
            // 创建新的转码线程
            if(cameraDto.isEnabledFFmpeg()) {
                log.info("创建 FFmpeg 转码线程");
                MediaTransferFlvByFFmpeg mediaft = new MediaTransferFlvByFFmpeg(cameraDto);
                mediaft.execute();
                cameras.put(cameraDto.getMediaKey(), mediaft);
                mediaft.addClient(ctx, ClientType.HTTP);
            } else {
                log.info("创建 JavaCV 转码线程");
                MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(cameraDto);
                cameras.put(cameraDto.getMediaKey(), mediaConvert);
                ThreadUtil.execute(mediaConvert);
                mediaConvert.addClient(ctx, ClientType.HTTP);
            }
        }
    }

    /**
     * WebSocket-FLV 播放
     * @param cameraDto 相机 DTO
     * @param ctx 通道上下文
     */
    public void playForWs(CameraDto cameraDto, ChannelHandlerContext ctx) {

        if (globalConfig != null) {
            applyGlobalConfig(cameraDto);
        }

        if (cameras.containsKey(cameraDto.getMediaKey())) {
            MediaTransfer mediaConvert = cameras.get(cameraDto.getMediaKey());

            if(mediaConvert instanceof MediaTransferFlvByJavacv) {
                MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;

                // 如果当前使用 JavaCV，但要求用 FFmpeg，则重新拉流
                if(cameraDto.isEnabledFFmpeg()) {
                    log.info("切换到 FFmpeg 模式");
                    mediaTransferFlvByJavacv.setRunning(false);
                    cameras.remove(cameraDto.getMediaKey());
                    this.playForWs(cameraDto, ctx);
                } else {
                    mediaTransferFlvByJavacv.addClient(ctx, ClientType.WEBSOCKET);
                }

            } else if (mediaConvert instanceof MediaTransferFlvByFFmpeg) {
                MediaTransferFlvByFFmpeg mediaTransferFlvByFFmpeg = (MediaTransferFlvByFFmpeg) mediaConvert;

                // 如果当前使用 FFmpeg，但要求用 JavaCV，则关闭再重新拉流
                if(!cameraDto.isEnabledFFmpeg()) {
                    log.info("切换到 JavaCV 模式");
                    mediaTransferFlvByFFmpeg.stopFFmpeg();
                    cameras.remove(cameraDto.getMediaKey());
                    this.playForWs(cameraDto, ctx);
                } else {
                    mediaTransferFlvByFFmpeg.addClient(ctx, ClientType.WEBSOCKET);
                }
            }
        } else {
            // 创建新的转码线程
            if(cameraDto.isEnabledFFmpeg()) {
                log.info("创建 FFmpeg 转码线程");
                MediaTransferFlvByFFmpeg mediaft = new MediaTransferFlvByFFmpeg(cameraDto);
                mediaft.execute();
                cameras.put(cameraDto.getMediaKey(), mediaft);
                mediaft.addClient(ctx, ClientType.WEBSOCKET);
            } else {
                log.info("创建 JavaCV 转码线程");
                MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(cameraDto);
                cameras.put(cameraDto.getMediaKey(), mediaConvert);
                ThreadUtil.execute(mediaConvert);
                mediaConvert.addClient(ctx, ClientType.WEBSOCKET);
            }
        }
    }

    /**
     * API 播放（无人观看自动关闭）
     * @param cameraDto 相机 DTO
     * @return 播放是否成功
     */
    public String playForApi(CameraDto cameraDto) {
        if (globalConfig != null) {
            applyGlobalConfig(cameraDto);
        }

        // 生成媒体 Key
        String mediaKey = MD5.create().digestHex(cameraDto.getUrl());
        cameraDto.setMediaKey(mediaKey);
        cameraDto.setEnabledFlv(true);
        // 生成或获取 Stream ID
        String streamId = StreamIdManager.generateStreamId(mediaKey);
        cameraDto.setStreamId(streamId);

        MediaTransfer mediaTransfer = cameras.get(cameraDto.getMediaKey());

        // 如果不存在，创建新的转码线程
        if (null == mediaTransfer) {
            if(cameraDto.isEnabledFFmpeg()) {
                log.info("创建 FFmpeg 转码线程（API）");
                MediaTransferFlvByFFmpeg mediaft = new MediaTransferFlvByFFmpeg(cameraDto);
                mediaft.execute();
                cameras.put(cameraDto.getMediaKey(), mediaft);
                mediaTransfer = mediaft;  // 直接使用创建的对象
            } else {
                log.info("创建 JavaCV 转码线程（API）");
                MediaTransferFlvByJavacv mediaConvert = new MediaTransferFlvByJavacv(cameraDto);
                cameras.put(cameraDto.getMediaKey(), mediaConvert);
                ThreadUtil.execute(mediaConvert);
                mediaTransfer = mediaConvert;  // 直接使用创建的对象
            }
        }

//        // 重新获取（可能刚创建）
//        mediaTransfer = cameras.get(cameraDto.getMediaKey());

        if (null == mediaTransfer) {
            log.error("创建媒体传输对象失败");
            return null;
        }

        // 同步等待启动完成
        if(mediaTransfer instanceof MediaTransferFlvByJavacv) {
            MediaTransferFlvByJavacv mediaft = (MediaTransferFlvByJavacv) mediaTransfer;
            // 30 秒还没 true 认为启动不了
            for (int i = 0; i < 60; i++) {
                if (mediaft.isRunning() && mediaft.isGrabberStatus() && mediaft.isRecorderStatus()) {
                    log.info("JavaCV 转码线程启动成功");
                    return streamId;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.error("JavaCV 转码线程启动超时");
        } else if (mediaTransfer instanceof MediaTransferFlvByFFmpeg) {
            MediaTransferFlvByFFmpeg mediaft = (MediaTransferFlvByFFmpeg) mediaTransfer;
            // 30 秒还没 true 认为启动不了
            for (int i = 0; i < 60; i++) {
                if (mediaft.isRunning()) {
                    log.info("FFmpeg 转码线程启动成功");
                    return streamId;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.error("FFmpeg 转码线程启动超时");
        }

        return null;
    }

    /**
     * 关闭流
     * @param cameraDto 相机 DTO
     */
    public void closeForApi(CameraDto cameraDto) {
        cameraDto.setEnabledFlv(false);

        if (cameras.containsKey(cameraDto.getMediaKey())) {
            MediaTransfer mediaConvert = cameras.get(cameraDto.getMediaKey());

            if(mediaConvert instanceof MediaTransferFlvByJavacv) {
                MediaTransferFlvByJavacv mediaTransferFlvByJavacv = (MediaTransferFlvByJavacv) mediaConvert;
                mediaTransferFlvByJavacv.setRunning(false);
                cameras.remove(cameraDto.getMediaKey());
                StreamIdManager.removeMapping(cameraDto.getMediaKey());
                log.info("已关闭 JavaCV 流：{}", cameraDto.getMediaKey());

            } else if (mediaConvert instanceof MediaTransferFlvByFFmpeg) {
                MediaTransferFlvByFFmpeg mediaTransferFlvByFFmpeg = (MediaTransferFlvByFFmpeg) mediaConvert;
                mediaTransferFlvByFFmpeg.stopFFmpeg();
                cameras.remove(cameraDto.getMediaKey());
                StreamIdManager.removeMapping(cameraDto.getMediaKey());
                log.info("已关闭 FFmpeg 流：{}", cameraDto.getMediaKey());
            }
        } else {
            log.warn("流不存在，无需关闭：{}", cameraDto.getMediaKey());
        }
    }

    /**
     * 应用全局配置到 CameraDto
     */
    private void applyGlobalConfig(CameraDto cameraDto) {
        if (cameraDto.getNetTimeout() <0) {
            cameraDto.setNetTimeout(globalConfig.getNetTimeout());
        }
        if (cameraDto.getReadOrWriteTimeout()<0) {
            cameraDto.setReadOrWriteTimeout(globalConfig.getReadOrWriteTimeout());
        }
        if (!cameraDto.isAutoClose()) {
            cameraDto.setAutoClose(globalConfig.isAutoClose());
        }
        if (cameraDto.getNoClientsDuration() <= 0) {
            cameraDto.setNoClientsDuration(globalConfig.getNoClientsDuration());
        }
    }

    /**
     * 获取活跃的流数量
     * @return 活跃流数量
     */
    public int getActiveStreamCount() {
        return cameras.size();
    }

    /**
     * 获取所有活跃的流信息
     * @return 流信息数组
     */
    public String[] getActiveStreams() {
        return cameras.keySet().toArray(new String[0]);
    }

    /**
     * 检查流是否存在
     * @param mediaKey 媒体 Key
     * @return 是否存在
     */
    public boolean hasStream(String mediaKey) {
        return cameras.containsKey(mediaKey);
    }
}
