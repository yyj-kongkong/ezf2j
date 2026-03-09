package com.ezf2js.autoconfigure;

import com.ez2fj.EZF2JEngine;
import com.ez2fj.common.EZF2JConfig;
import com.ez2fj.init.InitConfig;
import com.ez2fj.server.FlvHandler;
import com.ez2fj.server.MediaServer;
import com.ez2fj.service.CameraService;
import com.ez2fj.service.HlsService;
import com.ez2fj.service.MediaService;
import com.ezf2js.config.MediaServerProperties;
import com.ezf2js.engine.SpringMediaEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@ConditionalOnClass({MediaService.class, MediaServer.class, CameraService.class})
@EnableConfigurationProperties(MediaServerProperties.class)
@ConditionalOnProperty(prefix = "ezf2j.media", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MediaServerAutoConfiguration {

    @Autowired
    private MediaServerProperties properties;

    @Bean
    @ConditionalOnMissingBean(InitConfig.class)
    public InitConfig initConfig() {
        log.info("📋 初始化 EZF2J 配置...");

        InitConfig config = InitConfig.builder()
                .port(properties.getPort())
                .serverName(properties.getServerName())
                .flvPath(properties.getFlvPath())
                .hlsPath(properties.getHlsPath())
                .ffmpegPath(properties.getFfmpegPath())
                .readOrWriteTimeout(properties.getReadOrWriteTimeout())
                .netTimeout(properties.getNetTimeout())
                .autoClose(properties.isAutoClose())
                .noClientsDuration(properties.getNoClientsDuration())
                .enableFFmpeg(properties.isEnableFFmpeg())
                .enableHls(properties.isEnableHls())
                .build();

        if (properties.getThreadPoolCoreSize() != null && properties.getThreadPoolMaxSize() != null) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    properties.getThreadPoolCoreSize(),
                    properties.getThreadPoolMaxSize(),
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingDeque<>()
            );
            config.setThreadPoolExecutor(executor);
            log.info("   自定义线程池：core={}, max={}", 
                    properties.getThreadPoolCoreSize(), properties.getThreadPoolMaxSize());
        }

        return config;
    }

    @Bean
    @ConditionalOnMissingBean(FlvHandler.class)
    public FlvHandler flvHandler() {
        log.debug("创建 FlvHandler Bean");
        return new FlvHandler();
    }

    @Bean
    @ConditionalOnMissingBean(MediaService.class)
    public MediaService mediaService() {
        log.debug("创建 MediaService Bean");
        return new MediaService();
    }

    @Bean
    @ConditionalOnMissingBean(HlsService.class)
    public HlsService hlsService() {
        log.debug("创建 HlsService Bean");
        return HlsService.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean(CameraService.class)
    public CameraService cameraService() {
        log.debug("创建CameraService Bean");
        return CameraService.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean(MediaServer.class)
    public MediaServer mediaServer(FlvHandler flvHandler) {
        log.debug("创建 MediaServer Bean");
        return new MediaServer(flvHandler);
    }

    @Bean
    @ConditionalOnMissingBean(SpringMediaEngine.class)
    public SpringMediaEngine springMediaEngine(InitConfig initConfig,
                                                MediaService mediaService,
                                                HlsService hlsService,
                                                CameraService cameraService,
                                                MediaServer mediaServer) {
        log.info("🚀 初始化 SpringMediaEngine...");

        EZF2JEngine.init(initConfig);

        SpringMediaEngine engine = new SpringMediaEngine();
        engine.setMediaService(mediaService);
        engine.setHlsService(hlsService);
        engine.setCameraService(cameraService);
        engine.setMediaServer(mediaServer);
        engine.setConfig(initConfig);

        log.info("✅ SpringMediaEngine 初始化完成");
        log.info("   端口：{}, 服务名称：{}", properties.getPort(), properties.getServerName());
        log.info("   FLV 路径：{}, HLS 路径：{}", properties.getFlvPath(), properties.getHlsPath());
        log.info("   启用 FFmpeg: {}, 启用 HLS: {}", properties.isEnableFFmpeg(), properties.isEnableHls());
        
        return engine;
    }
}
