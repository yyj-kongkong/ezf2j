package com.ezf2jc.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FLV 流媒体服务器
 * 基于 Netty 实现 HTTP-FLV 和 WebSocket-FLV 服务
 *
 * @author ZJ
 */
@Slf4j
public class MediaServer {

    /**
     * FlvHandler 实例
     */
    private final FlvHandler flvHandler;

    /**
     * Boss 线程组
     */
    private EventLoopGroup bossGroup;

    /**
     * Worker 线程组
     */
    private EventLoopGroup workGroup;

    /**
     * 服务器通道
     */
    private Channel serverChannel;

    /**
     * 是否已启动
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 构造函数
     * @param flvHandler FlvHandler 实例
     */
    public MediaServer(FlvHandler flvHandler) {
        this.flvHandler = flvHandler;
    }

    /**
     * 启动服务器
     * @param socketAddress 监听地址
     */
    public void start(InetSocketAddress socketAddress) {
        if (started.get()) {
            log.warn("服务器已经启动，无需重复启动");
            return;
        }

        try {
            log.info("正在启动媒体服务器...");

            // 创建主线程组（1 个线程）
            bossGroup = new NioEventLoopGroup(1);
            // 创建工作线程组（200 个线程）
            workGroup = new NioEventLoopGroup(200);

            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // CORS 配置
                            CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
                                    .allowNullOrigin()
                                    .allowCredentials()
                                    .build();

                            // 配置管道
                            socketChannel.pipeline()
                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new ChunkedWriteHandler())
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    .addLast(new CorsHandler(corsConfig))
                                    .addLast(flvHandler);
                        }
                    })
                    .localAddress(socketAddress)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 首选直接内存
                    .option(ChannelOption.ALLOCATOR, io.netty.channel.unix.PreferredDirectByteBufAllocator.DEFAULT)
                    // TCP_NODELAY 禁用 Nagle 算法
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 保持连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 接收缓冲区大小
                    .childOption(ChannelOption.SO_RCVBUF, 128 * 1024)
                    // 发送缓冲区大小
                    .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                    // 写缓冲区水位线
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(1024 * 1024 / 2, 1024 * 1024));

            // 绑定端口并同步等待
            ChannelFuture future = bootstrap.bind(socketAddress).sync();
            serverChannel = future.channel();
            started.set(true);

            log.info("✅ 媒体服务器启动成功 - {}:{}", socketAddress.getHostString(), socketAddress.getPort());

            // 等待服务器通道关闭
            serverChannel.closeFuture().sync();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("服务器启动被中断", e);
        } catch (Exception e) {
            log.error("❌ 服务器启动失败", e);
            throw new RuntimeException("服务器启动失败：" + e.getMessage(), e);
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (!started.get()) {
            return;
        }

        try {
            log.info("正在关闭媒体服务器...");

            if (serverChannel != null) {
                serverChannel.close();
            }

            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }

            if (workGroup != null) {
                workGroup.shutdownGracefully();
            }

            started.set(false);
            log.info("✅ 媒体服务器已关闭");

        } catch (Exception e) {
            log.error("❌ 关闭服务器失败", e);
            throw new RuntimeException("关闭服务器失败：" + e.getMessage(), e);
        }
    }

    /**
     * 检查服务器是否运行中
     * @return 是否运行中
     */
    public boolean isRunning() {
        return started.get();
    }

    /**
     * 获取 FlvHandler 实例
     * @return FlvHandler
     */
    public FlvHandler getFlvHandler() {
        return flvHandler;
    }
}
