package com.ezf2jc.loader;

import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.engine.EngineConfig;
import com.ezf2jc.engine.MediaEngine;
import com.ezf2jc.engine.StreamManager;
import com.ezf2jc.server.FlvHandler;
import com.ezf2jc.server.MediaServer;
import com.ezf2jc.service.HlsService;
import com.ezf2jc.service.MediaService;
import com.ezf2jc.thread.MediaListenThread;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Loader;
import com.ezf2jc.common.MediaConstant;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MediaServerLoader {

    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    private static MediaEngine mediaEngine;

    private static StreamManager streamManager;

    private static MediaServer mediaServer;

    private static FlvHandler flvHandler;

    private static MediaService mediaService;

    private static HlsService hlsService;

    private static BaseMediaConfig config;

    private MediaServerLoader() {
    }

    public static MediaEngine load() {
        return load(new com.ezf2jc.config.DefaultMediaConfig());
    }

    public static synchronized MediaEngine load(BaseMediaConfig config) {
        if (loaded.get()) {
            log.warn("媒体服务器已经加载，无需重复加载");
            return mediaEngine;
        }

        try {
            log.info("正在初始化媒体服务器...");

            if (!config.validate()) {
                throw new RuntimeException("配置验证失败");
            }

            MediaServerLoader.config = config;

            initFFmpeg();

            createComponents(config);

            applyGlobalConfig(config);

            startListenThread();

            startNettyServer(config);

            createEngineAndManager();

            loaded.set(true);

            log.info("✅ 媒体服务器加载成功 - 地址:{}:{}, 自动关闭:{}, 关闭时长:{}ms",
                    config.getHost(), config.getPort(),
                    config.isAutoClose(), config.getNoClientsDuration());

            return mediaEngine;

        } catch (Exception e) {
            log.error("❌ 媒体服务器加载失败", e);
            throw new RuntimeException("加载失败：" + e.getMessage(), e);
        }
    }

    private static void initFFmpeg() {
        try {
            log.info("初始化 FFmpeg...");
            String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            System.setProperty(MediaConstant.FFMPEG_PATH_KEY, ffmpeg);
            log.info("✅ FFmpeg 初始化成功，路径：{}", ffmpeg);
        } catch (Exception e) {
            log.error("❌ FFmpeg 初始化失败", e);
            throw new RuntimeException("FFmpeg 初始化失败", e);
        }
    }

    private static void createComponents(BaseMediaConfig config) {
        flvHandler = new FlvHandler();
        mediaService = new MediaService();
        hlsService = new HlsService();
        mediaServer = new MediaServer(flvHandler);
    }

    private static void applyGlobalConfig(BaseMediaConfig config) {
        com.ezf2jc.config.MediaConfig mediaConfig =
                new com.ezf2jc.config.MediaConfig();
        mediaConfig.setPort(config.getPort());
        mediaConfig.setHost(config.getHost());
        mediaConfig.setNetTimeout(config.getNetTimeout());
        mediaConfig.setReadOrWriteTimeout(config.getReadOrWriteTimeout());
        mediaConfig.setAutoClose(config.isAutoClose());
        mediaConfig.setNoClientsDuration(config.getNoClientsDuration());
        mediaConfig.setEnableFFmpeg(config.isEnableFFmpeg());
        mediaConfig.setFfmpegPath(config.getFfmpegPath());
        mediaConfig.setLogLevel(config.getLogLevel());

        MediaService.setGlobalConfig(mediaConfig);

        HlsService.setDefaultPort(config.getHlsConfig().getPort());
        com.ezf2jc.thread.MediaTransferHls.setHlsHost(
                config.getHlsConfig().getHost());
    }

    private static void startListenThread() {
        Thread listenThread = new Thread(new MediaListenThread());
        listenThread.setDaemon(true);
        listenThread.setName("MediaListenThread");
        listenThread.start();
        log.debug("监听线程已启动");
    }

    private static void startNettyServer(BaseMediaConfig config) {
        Thread serverThread = new Thread(() -> {
            mediaServer.start(new InetSocketAddress(config.getHost(), config.getPort()));
        });
        serverThread.setDaemon(true);
        serverThread.setName("MediaServer-Thread");
        serverThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.debug("Netty 服务器已启动在 {}:{}" , config.getHost(), config.getPort());
    }

    private static void createEngineAndManager() {
        mediaEngine = new MediaEngine(mediaService, config);
        streamManager = new StreamManager(mediaService);
        log.debug("MediaEngine 和 StreamManager 已创建");
    }

    public static MediaEngine getMediaEngine() {
        checkLoaded();
        return mediaEngine;
    }

    public static StreamManager getStreamManager() {
        checkLoaded();
        return streamManager;
    }

    public static MediaServer getMediaServer() {
        checkLoaded();
        return mediaServer;
    }

    public static MediaService getMediaService() {
        checkLoaded();
        return mediaService;
    }

    public static HlsService getHlsService() {
        checkLoaded();
        return hlsService;
    }

    public static BaseMediaConfig getConfig() {
        checkLoaded();
        return config;
    }

    private static void checkLoaded() {
        if (!loaded.get()) {
            throw new IllegalStateException("媒体服务器未加载，请先调用 MediaServerLoader.load()");
        }
    }

    public static boolean isRunning() {
        return loaded.get();
    }

    public static synchronized void stop() {
        if (!loaded.get()) {
            log.warn("媒体服务器未运行，无需停止");
            return;
        }

        try {
            log.info("正在停止媒体服务器...");

            if (streamManager != null) {
                streamManager.stopAllStreams();
            }

            if (mediaServer != null) {
                mediaServer.shutdown();
            }

            MediaService.cameras.clear();

            loaded.set(false);
            mediaEngine = null;
            streamManager = null;
            mediaServer = null;
            flvHandler = null;
            mediaService = null;
            hlsService = null;
            config = null;

            log.info("✅ 媒体服务器已停止");

        } catch (Exception e) {
            log.error("❌ 停止媒体服务器失败", e);
            throw new RuntimeException("停止失败：" + e.getMessage(), e);
        }
    }
}
