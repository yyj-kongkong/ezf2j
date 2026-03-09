package com.ez2fj.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * flv 流媒体服务
 * 
 * @author ZJ
 *
 */
@Slf4j
public class MediaServer {

	private FlvHandler flvHandler;
	
	private Channel channel;
	
	private EventLoopGroup bossGroup;
	
	private EventLoopGroup workGroup;
	
	public MediaServer(FlvHandler flvHandler) {
		this.flvHandler = flvHandler;
	}
	
	public MediaServer() {
		this.flvHandler = new FlvHandler();
	}

    public void start(InetSocketAddress socketAddress) {
        //new 一个主线程组
        bossGroup = new NioEventLoopGroup(1);
        //new 一个工作线程组
        workGroup = new NioEventLoopGroup(200);
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();

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
                //首选直接内存
                .option(ChannelOption.ALLOCATOR, PreferredDirectByteBufAllocator.DEFAULT)
                //设置队列大小
//                .option(ChannelOption.SO_BACKLOG, 1024)
                // 两小时内没有数据的通信时,TCP 会自动发送一个活动探测数据报文
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_RCVBUF, 128 * 1024)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024 / 2, 1024 * 1024));
        //绑定端口，开始接收进来的连接
        try {
            ChannelFuture future = bootstrap.bind(socketAddress).sync();
            this.channel = future.channel();
            log.info("MediaServer 启动成功 - 端口：{}", socketAddress.getPort());
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("MediaServer 启动失败", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 停止服务器
     * 优雅关闭所有连接和资源
     */
    public void stop() {
        log.info("正在停止 MediaServer...");
        
        try {
            if (this.channel != null) {
                this.channel.close();
                log.info("主通道已关闭");
            }
            
            if (this.bossGroup != null) {
                this.bossGroup.shutdownGracefully(3, 10, TimeUnit.SECONDS);
                log.info("Boss 线程组已关闭");
            }
            
            if (this.workGroup != null) {
                this.workGroup.shutdownGracefully(3, 10, TimeUnit.SECONDS);
                log.info("Work 线程组已关闭");
            }
            
            log.info("MediaServer 已完全停止");
        } catch (Exception e) {
            log.error("停止 MediaServer 时出错", e);
        }
    }
    
    /**
     * 检查服务器是否正在运行
     * @return 是否运行中
     */
    public boolean isRunning() {
        return this.channel != null && this.channel.isActive();
    }
}