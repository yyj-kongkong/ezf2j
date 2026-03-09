package com.ez2fj;


import cn.hutool.core.thread.ThreadUtil;
import com.ez2fj.common.EZF2JConfig;
import com.ez2fj.init.InitConfig;
import com.ez2fj.server.FlvHandler;
import com.ez2fj.server.MediaServer;
import com.ez2fj.service.CameraService;
import com.ez2fj.service.HlsService;
import com.ez2fj.service.MediaService;
import com.ez2fj.thread.MediaListenThread;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class EZF2JEngine {

    private static volatile EZF2JEngine instance;
    
    private boolean initialized = false;

    private MediaService mediaService;
    private HlsService hlsService;
    private FlvHandler flvHandler;
    private MediaServer mediaServer;
    private CameraService cameraService;

    private EZF2JEngine() {
        initServices();
    }

    public static EZF2JEngine getInstance() {
        if (instance == null) {
            synchronized (EZF2JEngine.class) {
                if (instance == null) {
                    instance = new EZF2JEngine();
                }
            }
            Map<String, String> config = EZF2JConfig.getConfig();
            log.info("EZF2JEngine 初始化成功");
            log.info("EZF2JConfig 配置：{}", config);
        }
        if (instance.initialized) {
            Map<String, String> config = EZF2JConfig.getConfig();
            log.info("EZF2JEngine 已经初始化");
            log.info("EZF2JConfig 配置：{}", config);
        }
        return instance;
    }

    public static EZF2JEngine init(InitConfig initConfig) {
        if (instance != null && instance.initialized) {
            log.warn("EZF2JEngine 已经初始化过，将shutdown后重启");
            return instance;
        }
        
        synchronized (EZF2JEngine.class) {
            if (instance == null) {
                // 应用配置
                applyConfig(initConfig);
                instance = new EZF2JEngine();
            }
            instance.initialized = true;
        }
        return instance;
    }
    
    /**
     * 应用配置到全局配置类
     * @param initConfig 初始化配置
     */
    private static void applyConfig(InitConfig initConfig) {
        log.info("正在应用配置...");
        
        EZF2JConfig.PORT = initConfig.getPort();
        EZF2JConfig.serverName = initConfig.getServerName();
        EZF2JConfig.FLV_PATH = initConfig.getFlvPath();
        EZF2JConfig.HLS_PATH = initConfig.getHlsPath();
        EZF2JConfig.ffmpegPathKey = initConfig.getFfmpegPath();
        EZF2JConfig.READ_OR_WRITE_TIMEOUT = initConfig.getReadOrWriteTimeout();
        EZF2JConfig.AUTO_CLOSE = initConfig.isAutoClose();
        EZF2JConfig.NO_CLIENTS_DURATION = initConfig.getNoClientsDuration();
        EZF2JConfig.NET_TIMEOUT = initConfig.getNetTimeout();
        EZF2JConfig.ENABLE_FFMPEG = initConfig.isEnableFFmpeg();
        EZF2JConfig.ENABLE_HLS = initConfig.isEnableHls();
        
        if (initConfig.getThreadPoolExecutor() != null) {
            EZF2JConfig.threadpool = initConfig.getThreadPoolExecutor();
        }
        
        log.info("配置应用成功：端口={}, 服务器名称={}, FLV 路径={}, HLS 路径={}, 启用 FFmpeg={}, 启用 HLS={}", 
            EZF2JConfig.PORT, EZF2JConfig.serverName, EZF2JConfig.FLV_PATH, 
            EZF2JConfig.HLS_PATH, EZF2JConfig.ENABLE_FFMPEG, EZF2JConfig.ENABLE_HLS);
    }
    
    /**
     * 停止服务
     */
    public void shutdown() {
        log.info("正在停止 EZF2J 服务...");
        
        try {
            if (cameraService != null) {
                cameraService.shutdown();
            }
            
            if (mediaServer != null) {
                mediaServer.stop();
                log.info("媒体服务器已停止");
            }
            
            if (EZF2JConfig.threadpool != null) {
                EZF2JConfig.threadpool.shutdown();
                if (!EZF2JConfig.threadpool.awaitTermination(10, TimeUnit.SECONDS)) {
                    EZF2JConfig.threadpool.shutdownNow();
                }
                log.info("线程池已关闭");
            }
            
            log.info("EZF2J 服务已停止");
        } catch (Exception e) {
            log.error("停止服务时出错", e);
        }
    }

    private void initServices() {
        log.info("开始初始化服务...");
        this.cameraService = CameraService.getInstance();
        this.mediaService = new MediaService();
        this.hlsService = HlsService.getInstance();
        this.flvHandler = new FlvHandler();
        this.mediaServer = new MediaServer(this.flvHandler);
        
        ThreadUtil.execute(new MediaListenThread());
        
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            log.info("本机 IP: {}", ip);
        } catch (UnknownHostException e) {
            log.error("获取本机 IP 失败", e);
        }
        
        mediaServer.start(new InetSocketAddress("0.0.0.0", EZF2JConfig.PORT));
        log.info("EZF2J 服务启动成功 - 端口：{}, FLV 路径：{}, HLS 路径：{}", 
            EZF2JConfig.PORT, EZF2JConfig.FLV_PATH, EZF2JConfig.HLS_PATH);
    }
}
