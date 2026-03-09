package com.ezf2jc.server;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.ezf2jc.common.MediaConstant;
import com.ezf2jc.config.MediaConfig;
import com.ezf2jc.dto.CameraDto;
import com.ezf2jc.dto.StreamIdManager;
import com.ezf2jc.service.MediaService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FLV 协议处理器
 * 处理 HTTP-FLV 和 WebSocket-FLV 请求
 *
 * @author ZJ
 */
@Slf4j
@Sharable
public class FlvHandler extends SimpleChannelInboundHandler<Object> {

    /**
     * MediaService 实例
     */
    private final MediaService mediaService;

    /**
     * WebSocket 握手器
     */
    private WebSocketServerHandshaker handshaker;

    /**
     * 网络超时时间（毫秒）
     */
    private long netTimeout = 15000000;

    /**
     * 读写超时时间（毫秒）
     */
    private long readOrWriteTimeout = 15000000;

    /**
     * 是否自动关闭无人观看的流
     */
    private boolean autoClose = true;

    /**
     * 无人观看多久后自动关闭（毫秒）
     */
    private long noClientsDuration = 60000;

    /**
     * 构造函数
     */
    public FlvHandler() {
        this.mediaService = new MediaService();
        loadGlobalConfig();
    }

    /**
     * 加载全局配置
     */
    private void loadGlobalConfig() {
        MediaConfig config = MediaService.getGlobalConfig();
        if (config != null) {
            this.netTimeout = config.getNetTimeout();
            this.readOrWriteTimeout = config.getReadOrWriteTimeout();
            this.autoClose = config.isAutoClose();
            this.noClientsDuration = config.getNoClientsDuration();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            QueryStringDecoder decoder = new QueryStringDecoder(req.uri());

            // 验证请求路径
            if (!"/live".equals(decoder.path())) {
                log.warn("无效的请求路径：{}", decoder.path());
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            // 构建 CameraDto
            CameraDto cameraDto = buildCamera(req.uri());

            if (StrUtil.isBlank(cameraDto.getUrl())) {
                log.warn("URL 参数为空");
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            if (StrUtil.isNotBlank(cameraDto.getStreamId())) {
                String mediaKey = StreamIdManager.getMediaKeyByStreamId(cameraDto.getStreamId());
                if (mediaKey == null) {
                    log.warn("无效的 Stream ID: {}", cameraDto.getStreamId());
                    sendError(ctx, HttpResponseStatus.NOT_FOUND);
                    return;
                }
                cameraDto.setMediaKey(mediaKey);
                log.debug("通过 Stream ID 访问：{} -> {}", cameraDto.getStreamId(), mediaKey);
            }
            // 判断是否为 WebSocket 请求
            if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
                // HTTP-FLV 请求
                sendFlvReqHeader(ctx);
                mediaService.playForHttp(cameraDto, ctx);

            } else {
                // WebSocket 升级请求
                WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true, 5 * 1024 * 1024);
                handshaker = factory.newHandshaker(req);

                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    HttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    rsp.headers().set(HttpHeaderNames.SERVER, MediaConstant.SERVER_NAME);
                    DefaultChannelPromise channelPromise = new DefaultChannelPromise(ctx.channel());

                    handshaker.handshake(ctx.channel(), req, rsp.headers(), channelPromise);
                    mediaService.playForWs(cameraDto, ctx);
                }
            }

        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketRequest(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("新连接建立：{}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("连接断开：{}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接异常：{}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    /**
     * 获取 WebSocket 位置
     */
    private String getWebSocketLocation(FullHttpRequest request) {
        String location = request.headers().get(HttpHeaderNames.HOST) + request.uri();
        return "ws://" + location;
    }

    /**
     * 发送 FLV 请求头
     */
    private void sendFlvReqHeader(ChannelHandlerContext ctx) {
        HttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        rsp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE).set(HttpHeaderNames.CONTENT_TYPE, "video/x-flv").set(HttpHeaderNames.ACCEPT_RANGES, "bytes").set(HttpHeaderNames.PRAGMA, "no-cache").set(HttpHeaderNames.CACHE_CONTROL, "no-cache").set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED).set(HttpHeaderNames.SERVER, MediaConstant.SERVER_NAME);

        ctx.writeAndFlush(rsp);
    }

    /**
     * 发送错误响应
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("请求地址有误：" + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 处理 WebSocket 请求
     */
    private void handleWebSocketRequest(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 关闭帧
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        // PING 帧
        if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        // 文本帧（忽略）
        if (frame instanceof TextWebSocketFrame) {
            return;
        }

        // 二进制帧（忽略）
        if (frame instanceof BinaryWebSocketFrame) {
            return;
        }
    }

    /**
     * 构建 CameraDto
     */
    // ... existing code ...
    /**
     * 构建 CameraDto（支持两种模式）
     * 模式 1：传统方式 - url=rtsp://xxx&&&autoClose=true&&&ffmpeg=true
     * 模式 2：安全方式 - streamId=aB3cD9（推荐）
     */
    private CameraDto buildCamera(String uri) {
        CameraDto cameraDto = new CameraDto();
        setConfig(cameraDto);

        // 解析查询参数
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        Map<String, List<String>> params = decoder.parameters();

        // 1. 优先处理 streamId（安全模式）
        String streamId = getFirstParam(params, "streamId");
        if (StrUtil.isNotBlank(streamId)) {
            cameraDto.setStreamId(streamId);

            // 通过 streamId 查找 mediaKey
            String mediaKey = StreamIdManager.getMediaKeyByStreamId(streamId);
            if (mediaKey != null) {
                cameraDto.setMediaKey(mediaKey);
                log.debug("使用 Stream ID 模式：{} -> {}", streamId, mediaKey);
            } else {
                log.warn("无效的 Stream ID: {}", streamId);
                // 不立即返回，允许降级到 url 模式
            }
            // 如果有 streamId，不需要再解析 url
            return cameraDto;
        }

        // 2. 传统模式：解析 url 参数
        String url = getFirstParam(params, "url");
        if (StrUtil.isNotBlank(url)) {
            cameraDto.setUrl(url);
        }

        // 3. 解析其他参数
        String autoClose = getFirstParam(params, "autoClose");
        if (StrUtil.isNotBlank(autoClose)) {
            cameraDto.setAutoClose(Convert.toBool(autoClose, true));
        }

        String ffmpeg = getFirstParam(params, "ffmpeg");
        if (StrUtil.isNotBlank(ffmpeg)) {
            cameraDto.setEnabledFFmpeg(Convert.toBool(ffmpeg, false));
        }

        String hls = getFirstParam(params, "hls");
        if (StrUtil.isNotBlank(hls)) {
            cameraDto.setEnabledHls(Convert.toBool(hls, false));
        }

        // 4. 判断是否为本地文件
        if (cameraDto.getUrl() != null && isLocalFile(cameraDto.getUrl())) {
            cameraDto.setType(1);
        }

        // 5. 生成媒体 Key（如果没有 streamId）
        if (StrUtil.isBlank(cameraDto.getStreamId()) && StrUtil.isNotBlank(cameraDto.getUrl())) {
            String mediaKey = MD5.create().digestHex(cameraDto.getUrl());
            cameraDto.setMediaKey(mediaKey);
        }

        return cameraDto;
    }

    /**
     * 获取第一个参数值
     */
    private String getFirstParam(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    /**
     * 设置默认配置
     */
    private void setConfig(CameraDto cameraDto) {
        cameraDto.setNetTimeout(netTimeout);
        cameraDto.setReadOrWriteTimeout(readOrWriteTimeout);
        cameraDto.setAutoClose(autoClose);
        cameraDto.setNoClientsDuration(noClientsDuration);
    }

    /**
     * 判断是否为本地文件
     */
    private boolean isLocalFile(String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) {
            return false;
        }

        String[] split = streamUrl.trim().split("\\:");
        if (split.length > 0 && split[0].length() <= 1) {
            return true;
        }
        return false;
    }
}
