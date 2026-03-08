package com.ezf2jc.thread;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegLogCallback;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体转换者基类
 * 所有转码线程的父类
 *
 * @author ZJ
 */
@Slf4j
public abstract class MediaTransfer {

    static {
        // 设置 FFmpeg 日志级别为 ERROR
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        FFmpegLogCallback.set();
    }

    /**
     * WebSocket 客户端
     */
    public ConcurrentHashMap<String, ChannelHandlerContext> wsClients = new ConcurrentHashMap<>();

    /**
     * HTTP 客户端
     */
    public ConcurrentHashMap<String, ChannelHandlerContext> httpClients = new ConcurrentHashMap<>();

    /**
     * WebSocket 客户端数量
     */
    public int wcSize = 0;

    /**
     * HTTP 客户端数量
     */
    public int hcSize = 0;

    /**
     * 无客户端时的计时（毫秒）
     */
    public long noClient = 0;

    /**
     * FLV header
     */
    public byte[] header = null;

    /**
     * 输出流，视频最终会输出到此
     */
    public ByteArrayOutputStream bos = new ByteArrayOutputStream();

    /**
     * 转码回调
     */
    @Getter
    @Setter
    protected TransferCallback transferCallback;

    /**
     * 检查是否有客户端（用于自动关流）
     * 由子类实现具体逻辑
     */
    public abstract void hasClient();

    /**
     * 获取总客户端数
     * @return 客户端总数
     */
    public int getTotalClients() {
        return wcSize + hcSize;
    }

    /**
     * 是否有人观看
     * @return 是否有人观看
     */
    public boolean hasViewers() {
        return !wsClients.isEmpty() || !httpClients.isEmpty();
    }

    /**
     * 清理所有客户端连接
     */
    public void clearClients() {
        wsClients.clear();
        httpClients.clear();
        wcSize = 0;
        hcSize = 0;
    }
}
