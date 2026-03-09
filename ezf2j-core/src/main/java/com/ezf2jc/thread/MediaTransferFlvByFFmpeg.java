package com.ezf2jc.thread;


import cn.hutool.core.collection.CollUtil;
import com.ezf2jc.common.ClientType;
import com.ezf2jc.common.MediaConstant;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.service.MediaService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * FFmpeg FLV 转码线程
 * 使用 FFmpeg 进程进行转码
 *
 * @author ZJ
 */
@Slf4j
public class MediaTransferFlvByFFmpeg extends MediaTransfer {

    /**
     * 相机 DTO
     */
    private final CameraDto cameraDto;

    /**
     * FFmpeg 命令
     */
    private final List<String> command = new ArrayList<>();

    /**
     * TCP 服务器
     */
    private ServerSocket tcpServer = null;

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
     * 输出流处理线程
     */
    private Thread outputThread;

    /**
     * 运行状态
     */
    private volatile boolean running = false;

    /**
     * 是否启用日志
     */
    private boolean enableLog = true;

    /**
     * 记录当前时间（用于检测网络超时）
     */
    private volatile long currentTimeMillis = System.currentTimeMillis();

    /**
     * 构造函数
     */
    public MediaTransferFlvByFFmpeg(CameraDto cameraDto) {
        this.cameraDto = cameraDto;
        command.add(System.getProperty(MediaConstant.FFMPEG_PATH_KEY));
        buildCommand();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * 添加参数
     */
    private MediaTransferFlvByFFmpeg addArgument(String argument) {
        command.add(argument);
        return this;
    }

    /**
     * 构建 FFmpeg 转码命令
     */
    private void buildCommand() {
        // RTSP 流配置
        if (cameraDto.getUrl().startsWith("rtsp")) {
            this.addArgument("-rtsp_transport").addArgument("tcp");
        }

        // 本地文件配置
        if (cameraDto.getType() == 1) {
            this.addArgument("-re");
        }

        // 转码命令
        this.addArgument("-i").addArgument(cameraDto.getUrl())
                .addArgument("-max_delay").addArgument("1")
                .addArgument("-g").addArgument("25")
                .addArgument("-r").addArgument("25")
                .addArgument("-c:v").addArgument("libopenh264")
                .addArgument("-preset:v").addArgument("ultrafast")
                .addArgument("-tune:v").addArgument("zerolatency")
                .addArgument("-c:a").addArgument("aac")
                .addArgument("-f").addArgument("flv");
    }

    /**
     * 执行推流
     */
    public MediaTransferFlvByFFmpeg execute() {
        String output = getOutput();
        command.add(output);

        String join = CollUtil.join(command, " ");
        log.info("FFmpeg 命令：{}", join);

        try {
            process = new ProcessBuilder(command).start();
            running = true;
            listenNetTimeout();
            dealStream(process);
            outputData();
            listenClient();
        } catch (IOException e) {
            log.error("启动 FFmpeg 进程失败", e);
            running = false;
        }

        return this;
    }

    /**
     * 输出 FLV 数据
     */
    private void outputData() {
        outputThread = new Thread(() -> {
            Socket client = null;
            try {
                client = tcpServer.accept();
                DataInputStream input = new DataInputStream(client.getInputStream());

                byte[] buffer = new byte[1024];
                int len;

                while (running) {
                    len = input.read(buffer);

                    if (len == -1) {
                        break;
                    }

                    bos.write(buffer, 0, len);

                    if (header == null) {
                        header = bos.toByteArray();
                        bos.reset();
                        continue;
                    }

                    byte[] data = bos.toByteArray();
                    bos.reset();
                    sendFrameData(data);
                }

            } catch (SocketTimeoutException e1) {
                // 超时正常
            } catch (IOException e) {
                log.error("读取 FFmpeg 输出失败", e);
            } finally {
                MediaService.cameras.remove(cameraDto.getMediaKey());
                running = false;

                if (process != null) {
                    process.destroy();
                }

                try {
                    if (client != null) {
                        client.close();
                    }
                } catch (IOException e) {
                }
                try {
                    if (tcpServer != null) {
                        tcpServer.close();
                    }
                } catch (IOException e) {
                }
                try {
                    bos.close();
                } catch (IOException e) {
                }

                log.info("关闭媒体流-ffmpeg，{} ", cameraDto.getUrl());
            }
        });

        outputThread.start();
    }

    /**
     * 监听客户端
     */
    public void listenClient() {
        MediaListenThread.putThread(this);
    }

    /**
     * 监听网络超时
     */
    public void listenNetTimeout() {
        Thread thread = new Thread(() -> {
            while (running) {
                if ((System.currentTimeMillis() - currentTimeMillis) > 15000) {
                    log.warn("网络超时超过 15 秒");
                    stopFFmpeg();
                    break;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
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
                    currentTimeMillis = System.currentTimeMillis();
                    if (enableLog) {
                        log.debug("FFmpeg output: {}", line);
                    }
                }
            } catch (IOException e) {
                if (running) log.error("读取输出流失败", e);
            }
        });

        errThread = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while (running && (line = err.readLine()) != null) {
                    currentTimeMillis = System.currentTimeMillis();
                    if (enableLog) {
                        log.info("FFmpeg: {}", line);
                    }
                }
            } catch (IOException e) {
                if (running) log.error("读取错误流失败", e);
            }
        });

        inputThread.start();
        errThread.start();
    }

    /**
     * 获取输出地址（TCP）
     */
    private String getOutput() {
        try {
            tcpServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            int port = tcpServer.getLocalPort();
            String host = tcpServer.getInetAddress().getHostAddress();
            tcpServer.setSoTimeout(10000);
            return "tcp://" + host + ":" + port;
        } catch (IOException e) {
            log.error("创建 TCP 服务器失败", e);
            throw new RuntimeException("无法启用端口", e);
        }
    }

    /**
     * 关闭 FFmpeg
     */
    public void stopFFmpeg() {
        running = false;

        try {
            if (process != null) {
                process.destroy();
                log.info("FFmpeg 进程已停止");
            }
        } catch (Exception e) {
            log.error("停止 FFmpeg 失败", e);
            if (process != null) {
                process.destroyForcibly();
            }
        }

        // 关闭客户端连接
        for (ChannelHandlerContext ctx : wsClients.values()) {
            try {
                ctx.close();
            } catch (Exception e) {
            }
        }
        for (ChannelHandlerContext ctx : httpClients.values()) {
            try {
                ctx.close();
            } catch (Exception e) {
            }
        }

        clearClients();
        MediaService.cameras.remove(cameraDto.getMediaKey());
        MediaListenThread.removeThread(this);
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

        if (!cameraDto.isAutoClose()) {
            return;
        }

        if (httpClients.isEmpty() && wsClients.isEmpty()) {
            if (noClient > cameraDto.getNoClientsDuration()) {
                log.info("无人观看超过{}ms，自动关闭流", cameraDto.getNoClientsDuration());
                stopFFmpeg();
            } else {
                noClient += 1000;
            }
        } else {
            noClient = 0;
        }
    }

    /**
     * 发送帧数据
     */
    private void sendFrameData(byte[] data) {
        // WebSocket 客户端
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

        // HTTP 客户端
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
}
