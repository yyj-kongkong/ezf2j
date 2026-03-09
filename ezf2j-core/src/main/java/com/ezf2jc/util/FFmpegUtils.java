package com.ezf2jc.util;


import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Loader;
import com.ezf2jc.common.MediaConstant;

import java.io.File;

/**
 * FFmpeg 工具类
 *
 * @author ZJ
 */
@Slf4j
public class FFmpegUtils {

    /**
     * 初始化 FFmpeg 并设置路径
     * @return FFmpeg 路径
     */
    public static String initFFmpeg() {
        try {
            String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            System.setProperty(MediaConstant.FFMPEG_PATH_KEY, ffmpeg);
            log.info("FFmpeg 初始化成功，路径：{}", ffmpeg);
            return ffmpeg;
        } catch (Exception e) {
            log.error("FFmpeg 初始化失败", e);
            throw new RuntimeException("FFmpeg 初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 检查 FFmpeg 是否可用
     * @return 是否可用
     */
    public static boolean isFFmpegAvailable() {
        String path = System.getProperty(MediaConstant.FFMPEG_PATH_KEY);
        if (path == null || path.isEmpty()) {
            return false;
        }

        File file = new File(path);
        return file.exists() && file.canExecute();
    }

    /**
     * 获取 FFmpeg 版本信息
     * @return 版本信息
     */
    public static String getFFmpegVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    System.getProperty(MediaConstant.FFMPEG_PATH_KEY),
                    "-version"
            );
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder version = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                version.append(line).append("\n");
            }

            process.waitFor();
            return version.toString().trim();
        } catch (Exception e) {
            return "无法获取版本信息";
        }
    }

    /**
     * 设置 FFmpeg 路径
     * @param path FFmpeg 可执行文件路径
     */
    public static void setFFmpegPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("FFmpeg 路径不能为空");
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("FFmpeg 文件不存在：" + path);
        }

        if (!file.canExecute()) {
            throw new IllegalArgumentException("FFmpeg 文件不可执行：" + path);
        }

        System.setProperty(MediaConstant.FFMPEG_PATH_KEY, path);
        log.info("设置 FFmpeg 路径：{}", path);
    }

    /**
     * 获取 FFmpeg 路径
     * @return FFmpeg 路径
     */
    public static String getFFmpegPath() {
        return System.getProperty(MediaConstant.FFMPEG_PATH_KEY);
    }

    /**
     * 构建 FFmpeg 命令（基础）
     */
    public static String[] buildBasicCommand(String inputUrl, String outputUrl) {
        return new String[] {
                getFFmpegPath(),
                "-i", inputUrl,
                "-c:v", "libopenh264",
                "-c:a", "aac",
                "-f", "flv",
                outputUrl
        };
    }

    /**
     * 构建 RTSP 转 FLV 命令
     */
    public static String[] buildRtspToFlvCommand(String rtspUrl, String outputUrl) {
        return new String[] {
                getFFmpegPath(),
                "-rtsp_transport", "tcp",
                "-i", rtspUrl,
                "-c:v", "libopenh264",
                "-preset:v", "ultrafast",
                "-tune:v", "zerolatency",
                "-c:a", "aac",
                "-f", "flv",
                outputUrl
        };
    }

    /**
     * 构建 HLS 切片命令
     */
    public static String[] buildHlsCommand(String inputUrl, String outputM3u8,
                                           int time, int wrap) {
        return new String[] {
                getFFmpegPath(),
                "-i", inputUrl,
                "-c:v", "libopenh264",
                "-c:a", "aac",
                "-f", "hls",
                "-hls_time", String.valueOf(time),
                "-hls_wrap", String.valueOf(wrap),
                "-hls_list_size", "0",
                outputM3u8
        };
    }
}
