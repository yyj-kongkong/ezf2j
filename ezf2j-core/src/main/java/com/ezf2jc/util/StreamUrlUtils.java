package com.ezf2jc.util;


import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 流地址工具类
 *
 * @author ZJ
 */
public class StreamUrlUtils {

    /**
     * 判断是否为 RTSP 流
     */
    public static boolean isRtsp(String url) {
        return url != null && url.trim().toLowerCase().startsWith("rtsp");
    }

    /**
     * 判断是否为 RTMP 流
     */
    public static boolean isRtmp(String url) {
        return url != null && url.trim().toLowerCase().startsWith("rtmp");
    }

    /**
     * 判断是否为本地文件
     */
    public static boolean isLocalFile(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String[] parts = url.trim().split("\\:");
        if (parts.length > 0 && parts[0].length() <= 1) {
            return true;
        }

        return url.startsWith("/") || url.startsWith("\\");
    }

    /**
     * 判断是否为桌面采集
     */
    public static boolean isDesktop(String url) {
        return "desktop".equalsIgnoreCase(url);
    }

    /**
     * 获取流类型
     */
    public static StreamType getStreamType(String url) {
        if (isRtsp(url)) {
            return StreamType.RTSP;
        } else if (isRtmp(url)) {
            return StreamType.RTMP;
        } else if (isLocalFile(url)) {
            return StreamType.LOCAL_FILE;
        } else if (isDesktop(url)) {
            return StreamType.DESKTOP;
        }
        return StreamType.UNKNOWN;
    }

    /**
     * 生成 WebSocket 播放地址
     */
    public static String buildWsPlayUrl(String streamUrl, String host, int port) {
        return String.format("ws://%s:%d/live?url=%s", host, port, streamUrl);
    }

    /**
     * 生成 HTTP-FLV 播放地址
     */
    public static String buildHttpPlayUrl(String streamUrl, String host, int port) {
        return String.format("http://%s:%d/live?url=%s", host, port, streamUrl);
    }

    /**
     * 生成 HLS 播放地址
     */
    public static String buildHlsPlayUrl(String streamUrl, String host, int port) {
        return String.format("http://%s:%d/hls?url=%s", host, port, streamUrl);
    }

    /**
     * 获取本机 IP 地址
     */
    public static String getLocalIp() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * 流类型枚举
     */
    public enum StreamType {
        RTSP("RTSP"),
        RTMP("RTMP"),
        LOCAL_FILE("本地文件"),
        DESKTOP("桌面采集"),
        UNKNOWN("未知");

        private final String description;

        StreamType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
