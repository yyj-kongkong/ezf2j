package com.ezf2jc.thread;


import cn.hutool.core.collection.CollUtil;
import com.ezf2jc.common.MediaConstant;
import com.ezf2jc.dto.CameraDto;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * HLS 切片线程
 * 使用 FFmpeg 进行 HLS 切片
 *
 * @author ZJ
 */
@Slf4j
public class MediaTransferHls extends MediaTransfer {

    /**
     * 运行状态
     */
    private volatile boolean running = false;

    /**
     * FFmpeg 进程
     */
    private Process process;

    /**
     * 输入流处理线程
     */
    private Thread inputThread;

    /**
     * 错误流处理线程
     */
    private Thread errThread;

    /**
     * 端口
     */
    private int port = 8866;

    /**
     * 相机 DTO
     */
    private final CameraDto cameraDto;

    /**
     * FFmpeg 命令
     */
    private final List<String> command = new ArrayList<>();

    /**
     * HLS 主机地址
     */
    private static String hlsHost = "localhost";

    /**
     * 设置 HLS 主机
     */
    public static void setHlsHost(String host) {
        hlsHost = host;
    }

    /**
     * 构造函数
     */
    public MediaTransferHls(CameraDto cameraDto, int port) {
        this.cameraDto = cameraDto;
        this.port = port;
        buildCommand();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private MediaTransferHls addArgument(String argument) {
        command.add(argument);
        return this;
    }

    /**
     * 构建 FFmpeg HLS 命令
     */
    private void buildCommand() {
        command.add(System.getProperty(MediaConstant.FFMPEG_PATH_KEY));

        // 本地文件配置
        if (cameraDto.getType() == 1) {
            command.add("-re");
        }

        // HLS 切片命令
        command.add("-i");
        this.addArgument(cameraDto.getUrl())
                .addArgument("-r").addArgument("25")
                .addArgument("-g").addArgument("25")
                .addArgument("-c:v").addArgument("libopenh264")
                .addArgument("-c:a").addArgument("aac")
                .addArgument("-f").addArgument("hls")
                .addArgument("-hls_list_size").addArgument("1")
                .addArgument("-hls_wrap").addArgument("6")
                .addArgument("-hls_time").addArgument("1")
                .addArgument("-hls_base_url").addArgument("/ts/" + cameraDto.getMediaKey() + "/")
                .addArgument("-method").addArgument("put")
                .addArgument("http://" + hlsHost + ":" + port + "/record/" + cameraDto.getMediaKey() + "/out.m3u8");
    }

    /**
     * 执行
     */
    public void execute() {
        String join = CollUtil.join(command, " ");
        log.info("HLS FFmpeg 命令：{}", join);

        try {
            process = new ProcessBuilder(command).start();
            running = true;
            dealStream(process);
            log.info("HLS 切片线程已启动");
        } catch (IOException e) {
            running = false;
            log.error("启动 HLS 切片失败", e);
        }
    }

    /**
     * 关闭
     */
    public void stop() {
        this.running = false;
        try {
            if (process != null) {
                process.destroy();
                log.info("HLS 切片已停止");
            }
        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 处理 FFmpeg 输出流
     */
    private void dealStream(Process process) {
        if (process == null) return;

        inputThread = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (running && (line = in.readLine()) != null) {
                    // 忽略输出
                }
            } catch (IOException e) {
                if (running) log.error("读取输出流失败", e);
            }
        });

        errThread = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while (running && (line = err.readLine()) != null) {
                    log.debug("HLS: {}", line);
                }
            } catch (IOException e) {
                if (running) log.error("读取错误流失败", e);
            }
        });

        inputThread.start();
        errThread.start();
    }

    @Override
    public void hasClient() {
        // HLS 不需要客户端检查
    }
}
