package com.ezf2jc.thread;


import cn.hutool.core.util.StrUtil;
import com.ezf2jc.common.ClientType;
import com.ezf2jc.config.MediaConfig;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.service.MediaService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.IOException;
import java.util.Map.Entry;

/**
 * JavaCV FLV 转码线程
 * 支持转封装或转码
 *
 * @author ZJ
 */
@Slf4j
public class MediaTransferFlvByJavacv extends MediaTransfer implements Runnable {

    /**
     * 运行状态
     */
    private volatile boolean running = false;

    /**
     * 拉流器状态
     */
    private boolean grabberStatus = false;

    /**
     * 录制器状态
     */
    private boolean recorderStatus = false;

    /**
     * 拉流器
     */
    private FFmpegFrameGrabber grabber;

    /**
     * 推流录制器
     */
    private FFmpegFrameRecorder recorder;

    /**
     * 转码标志：true=转复用，false=转码
     */
    private boolean transferFlag = false;

    /**
     * 相机 DTO
     */
    private final CameraDto cameraDto;

    /**
     * 构造函数
     * @param cameraDto 相机 DTO
     */
    public MediaTransferFlvByJavacv(CameraDto cameraDto) {
        this.cameraDto = cameraDto;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isGrabberStatus() {
        return grabberStatus;
    }

    public void setGrabberStatus(boolean grabberStatus) {
        this.grabberStatus = grabberStatus;
    }

    public boolean isRecorderStatus() {
        return recorderStatus;
    }

    public void setRecorderStatus(boolean recorderStatus) {
        this.recorderStatus = recorderStatus;
    }

    /**
     * 创建拉流器
     * @return 是否成功
     */
    protected boolean createGrabber() {
        grabber = new FFmpegFrameGrabber(cameraDto.getUrl());

        // 超时时间（15 秒）
        grabber.setOption("stimeout", ""+cameraDto.getNetTimeout());
        grabber.setOption("threads", "1");

        // 设置缓存大小，提高画质、减少卡顿
        grabber.setOption("buffer_size", "1024000");

        // 读写超时
        grabber.setOption("rw_timeout", ""+cameraDto.getReadOrWriteTimeout());

        // 探测视频流信息
        grabber.setOption("probesize", ""+cameraDto.getReadOrWriteTimeout());
        grabber.setOption("analyzeduration", ""+cameraDto.getReadOrWriteTimeout());

        // RTSP 特殊配置
        if (cameraDto.getUrl().startsWith("rtsp")) {
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setOption("rtsp_flags", "prefer_tcp");
        } else if (cameraDto.getUrl().startsWith("rtmp")) {
            grabber.setOption("rtmp_buffer", "1000");
        } else if ("desktop".equals(cameraDto.getUrl())) {
            // 桌面采集
            grabber.setFormat("gdigrab");
            grabber.setOption("draw_mouse", "1");
            grabber.setNumBuffers(0);
            grabber.setOption("fflags", "nobuffer");
            grabber.setFrameRate(25);
        }

        try {
            grabber.start();
            log.info("启动拉流器成功：{}", cameraDto.getUrl());
            return grabberStatus = true;
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            MediaService.cameras.remove(cameraDto.getMediaKey());
            log.error("启动拉流器失败：{}", cameraDto.getUrl(), e);
        }
        return grabberStatus = false;
    }

    /**
     * 创建转码推流录制器
     * @return 是否成功
     */
    protected boolean createTransterOrRecodeRecorder() {
        recorder = new FFmpegFrameRecorder(bos, grabber.getImageWidth(), grabber.getImageHeight(),
                grabber.getAudioChannels());
        recorder.setFormat("flv");

        if (!transferFlag) {
            // 转码模式
            recorder.setInterleaved(false);
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("crf", "26");
            recorder.setVideoOption("threads", "1");
            recorder.setFrameRate(25);
            recorder.setGopSize(25);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setOption("keyint_min", "25");
            recorder.setTrellis(1);
            recorder.setMaxDelay(0);

            try {
                recorder.start();
                return recorderStatus = true;
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
                log.error("启动转码录制器失败", e1);
                MediaService.cameras.remove(cameraDto.getMediaKey());
            }
        } else {
            // 转复用模式
            recorder.setCloseOutputStream(false);
            try {
                recorder.start(grabber.getFormatContext());
                return recorderStatus = true;
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                log.warn("启动转复用录制器失败，切换到转码模式", e);
                transferFlag = false;

                if (recorder != null) {
                    try {
                        recorder.stop();
                    } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {}
                }

                return createTransterOrRecodeRecorder();
            }
        }
        return recorderStatus = false;
    }

    /**
     * 是否支持 FLV 的音视频编码
     * @return 是否支持
     */
    private boolean supportFlvFormatCodec() {
        int audioChannels = grabber.getAudioChannels();
        int vcodec = grabber.getVideoCodec();
        int acodec = grabber.getAudioCodec();

        return (cameraDto.getType() == 0) &&
                ("desktop".equals(cameraDto.getUrl()) ||
                        avcodec.AV_CODEC_ID_H264 == vcodec || avcodec.AV_CODEC_ID_H263 == vcodec) &&
                (audioChannels == 0 ||
                        avcodec.AV_CODEC_ID_AAC == acodec || avcodec.AV_CODEC_ID_AAC_LATM == acodec);
    }

    /**
     * 将视频源转换为 FLV
     */
    protected void transferStream2Flv() {
        if (!createGrabber()) {
            return;
        }

        transferFlag = supportFlvFormatCodec();

        if (!createTransterOrRecodeRecorder()) {
            return;
        }

        try {
            grabber.flush();
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            log.error("清空拉流器缓存失败", e);
        }

        if (header == null) {
            header = bos.toByteArray();
            bos.reset();
        }

        running = true;

        // 启动监听线程（用于判断是否需要自动关闭推流）
        listenClient();

        // 时间戳计算
        long startTime = 0;
        long videoTS = 0;

        for (; running && grabberStatus && recorderStatus;) {
            try {
                if (transferFlag) {
                    // 转复用
                    long startGrab = System.currentTimeMillis();
                    AVPacket pkt = grabber.grabPacket();

                    if ((System.currentTimeMillis() - startGrab) > 5000) {
                        log.info("视频流网络异常");
                        closeMedia();
                        break;
                    }

                    if (null != pkt && !pkt.isNull()) {
                        if (startTime == 0) {
                            startTime = System.currentTimeMillis();
                        }
                        videoTS = 1000 * (System.currentTimeMillis() - startTime);

                        if (videoTS > recorder.getTimestamp()) {
                            recorder.setTimestamp(videoTS);
                        }
                        recorder.recordPacket(pkt);
                    }
                } else {
                    // 转码
                    long startGrab = System.currentTimeMillis();
                    Frame frame = grabber.grab();

                    if ((System.currentTimeMillis() - startGrab) > 5000) {
                        log.info("视频流网络异常");
                        closeMedia();
                        break;
                    }

                    if (frame != null) {
                        if (startTime == 0) {
                            startTime = System.currentTimeMillis();
                        }
                        videoTS = 1000 * (System.currentTimeMillis() - startTime);

                        if (videoTS > recorder.getTimestamp()) {
                            recorder.setTimestamp(videoTS);
                        }
                        recorder.record(frame);
                    }
                }
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                grabberStatus = false;
                MediaService.cameras.remove(cameraDto.getMediaKey());
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                recorderStatus = false;
                MediaService.cameras.remove(cameraDto.getMediaKey());
            }

            if (bos.size() > 0) {
                byte[] data = bos.toByteArray();
                bos.reset();
                sendFrameData(data);
            }
        }

        // 关闭资源
        try {
            recorder.close();
            grabber.close();
            bos.close();
        } catch (Exception e) {
            log.error("关闭资源失败", e);
        } finally {
            closeMedia();
        }

        log.info("关闭媒体流-JavaCV: {}", cameraDto.getUrl());
    }

    /**
     * 发送帧数据到客户端
     */
    private void sendFrameData(byte[] data) {
        // 发送给 WebSocket 客户端
        for (Entry<String, ChannelHandlerContext> entry : wsClients.entrySet()) {
            try {
                if (entry.getValue().channel().isWritable()) {
                    entry.getValue().writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(data)));
                } else {
                    wsClients.remove(entry.getKey());
                    hasClient();
                }
            } catch (Exception e) {
                wsClients.remove(entry.getKey());
                hasClient();
            }
        }

        // 发送给 HTTP 客户端
        for (Entry<String, ChannelHandlerContext> entry : httpClients.entrySet()) {
            try {
                if (entry.getValue().channel().isWritable()) {
                    entry.getValue().writeAndFlush(Unpooled.copiedBuffer(data));
                } else {
                    httpClients.remove(entry.getKey());
                    hasClient();
                }
            } catch (Exception e) {
                httpClients.remove(entry.getKey());
                hasClient();
            }
        }
    }

    /**
     * 监听客户端（用于自动关流）
     */
    public void listenClient() {
        MediaListenThread.putThread(this);
    }

    /**
     * 检查是否有客户端
     */
    @Override
    public void hasClient() {
        int newHcSize = httpClients.size();
        int newWcSize = wsClients.size();

        if (hcSize != newHcSize || wcSize != newWcSize) {
            hcSize = newHcSize;
            wcSize = newWcSize;
            log.info("HTTP 连接数:{}, WS 连接数:{}", newHcSize, newWcSize);
        }

        // 无需自动关闭
        if (!cameraDto.isAutoClose()) {
            return;
        }

        if (httpClients.isEmpty() && wsClients.isEmpty()) {
            // 无人观看，开始计时
            if (noClient > cameraDto.getNoClientsDuration()) {
                log.info("无人观看超过{}ms，自动关闭流", cameraDto.getNoClientsDuration());
                closeMedia();
            } else {
                noClient += 1000;
            }
        } else {
            // 有客户端，重置计时
            noClient = 0;
        }
    }

    /**
     * 关闭媒体流
     */
    private void closeMedia() {
        running = false;
        MediaService.cameras.remove(cameraDto.getMediaKey());

        // 关闭所有客户端连接
        for (ChannelHandlerContext ctx : wsClients.values()) {
            try {
                ctx.close();
            } catch (Exception e) {}
        }
        for (ChannelHandlerContext ctx : httpClients.values()) {
            try {
                ctx.close();
            } catch (Exception e) {}
        }

        clearClients();
        MediaListenThread.removeThread(this);
    }

    /**
     * 新增客户端
     */
    public void addClient(ChannelHandlerContext ctx, ClientType ctype) {
        int timeout = 0;

        while (true) {
            try {
                if (header != null) {
                    if (ctx.channel().isWritable()) {
                        if (ctype == ClientType.HTTP) {
                            ChannelFuture future = ctx.writeAndFlush(Unpooled.copiedBuffer(header));
                            future.addListener(f -> {
                                if (f.isSuccess()) {
                                    httpClients.put(ctx.channel().id().toString(), ctx);
                                }
                            });
                        } else if (ctype == ClientType.WEBSOCKET) {
                            ChannelFuture future = ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(header)));
                            future.addListener(f -> {
                                if (f.isSuccess()) {
                                    wsClients.put(ctx.channel().id().toString(), ctx);
                                }
                            });
                        }
                    }
                    break;
                }

                // 等待启动
                Thread.sleep(50);
                timeout += 50;

                if (timeout > 30000) {
                    log.error("等待 header 超时");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void run() {
        transferStream2Flv();
    }
}
