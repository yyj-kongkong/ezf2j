package com.ezf2jc.server;


import com.ezf2jc.common.CacheMap;
import com.ezf2jc.service.HlsService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * HLS 协议处理器
 * 处理 M3U8 和 TS 文件请求
 *
 * @author ZJ
 */
@Slf4j
@Sharable
public class HlsHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * TS 缓存
     */
    private final CacheMap<String, byte[]> tsCache;

    /**
     * M3U8 缓存
     */
    private final CacheMap<String, byte[]> m3u8Cache;

    /**
     * 构造函数
     */
    public HlsHandler() {
        this.tsCache = HlsService.cacheTs;
        this.m3u8Cache = HlsService.cacheM3u8;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri();

        // 处理 M3U8 请求
        if (uri.endsWith(".m3u8")) {
            handleM3u8Request(ctx, uri);
            return;
        }

        // 处理 TS 请求
        if (uri.endsWith(".ts")) {
            handleTsRequest(ctx, uri);
            return;
        }

        // 其他请求，传递给下一个处理器
        ctx.fireChannelRead(req);
    }

    /**
     * 处理 M3U8 请求
     */
    private void handleM3u8Request(ChannelHandlerContext ctx, String uri) {
        String[] parts = uri.split("/");
        if (parts.length < 2) {
            sendNotFound(ctx);
            return;
        }

        String mediaKey = parts[parts.length - 1].replace(".m3u8", "");
        byte[] content = m3u8Cache.get(mediaKey);

        if (content == null) {
            sendNotFound(ctx);
            return;
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/vnd.apple.mpegurl");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        log.debug("返回 M3U8: {}", uri);
    }

    /**
     * 处理 TS 请求
     */
    private void handleTsRequest(ChannelHandlerContext ctx, String uri) {
        String[] parts = uri.split("/");
        if (parts.length < 3) {
            sendNotFound(ctx);
            return;
        }

        String mediaKey = parts[parts.length - 2];
        String tsName = parts[parts.length - 1];
        String cacheKey = mediaKey + "-" + tsName;

        byte[] content = tsCache.get(cacheKey);

        if (content == null) {
            sendNotFound(ctx);
            return;
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "video/mp2t");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        log.debug("返回 TS: {}", uri);
    }

    /**
     * 发送 404 响应
     */
    private void sendNotFound(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NOT_FOUND,
                Unpooled.copiedBuffer("资源不存在", CharsetUtil.UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HLS 请求处理异常", cause);
        ctx.close();
    }
}
