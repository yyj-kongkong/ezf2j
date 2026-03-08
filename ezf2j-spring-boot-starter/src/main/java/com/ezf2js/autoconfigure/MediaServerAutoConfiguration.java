package com.ezf2js.autoconfigure;

import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.config.DefaultMediaConfig;
import com.ezf2jc.engine.MediaEngine;
import com.ezf2jc.engine.StreamManager;
import com.ezf2jc.loader.MediaServerLoader;
import com.ezf2jc.server.FlvHandler;
import com.ezf2jc.server.MediaServer;
import com.ezf2jc.service.HlsService;
import com.ezf2jc.service.MediaService;
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

/**
 * 媒体服务器自动配置
 * Spring Boot 启动时自动初始化媒体服务
 *
 * @author ZJ
 */
@Slf4j
@Configuration
@ConditionalOnClass({MediaService.class, MediaServer.class})
@EnableConfigurationProperties(MediaServerProperties.class)
@ConditionalOnProperty(prefix = "ezf2j.media", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MediaServerAutoConfiguration {

    /**
     * 配置属性
     */
    @Autowired
    private MediaServerProperties properties;

    /**
     * 创建媒体配置 Bean
     * @return BaseMediaConfig 实例
     */
    @Bean
    @ConditionalOnMissingBean(BaseMediaConfig.class)
    public BaseMediaConfig mediaConfig() {
        log.info("初始化媒体服务器配置...");

        DefaultMediaConfig config = new DefaultMediaConfig();

        // 从配置文件加载配置
        if (properties.getPort() != null) {
            config.setPort(properties.getPort());
        }
        if (properties.getHost() != null) {
            config.setHost(properties.getHost());
        }
        if (properties.getNetTimeout() >0) {
            config.setNetTimeout(properties.getNetTimeout());
        }
        if (properties.getReadOrWriteTimeout() >0) {
            config.setReadOrWriteTimeout(properties.getReadOrWriteTimeout());
        }
        if (properties.getAutoClose() != null) {
            config.setAutoClose(properties.getAutoClose());
        }
        if (properties.getNoClientsDuration() != null) {
            config.setNoClientsDuration(properties.getNoClientsDuration());
        }
        if (properties.getEnableFFmpeg() != null) {
            config.setEnableFFmpeg(properties.getEnableFFmpeg());
        }
        if (properties.getFfmpegPath() != null) {
            config.setFfmpegPath(properties.getFfmpegPath());
        }
        if (properties.getLogLevel() >0) {
            config.setLogLevel(properties.getLogLevel());
        }

        // HLS 配置
        if (properties.getHls() != null) {
            if (properties.getHls().getPort() != null) {
                config.getHlsConfig().setPort(properties.getHls().getPort());
            }
            if (properties.getHls().getHost() != null) {
                config.getHlsConfig().setHost(properties.getHls().getHost());
            }
        }

        return config;
    }

    /**
     * 创建 FlvHandler Bean
     * @return FlvHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean(FlvHandler.class)
    public FlvHandler flvHandler() {
        log.debug("创建 FlvHandler Bean");
        return new FlvHandler();
    }

    /**
     * 创建 MediaService Bean
     * @return MediaService 实例
     */
    @Bean
    @ConditionalOnMissingBean(MediaService.class)
    public MediaService mediaService() {
        log.debug("创建 MediaService Bean");
        return new MediaService();
    }

    /**
     * 创建 HlsService Bean
     * @return HlsService 实例
     */
    @Bean
    @ConditionalOnMissingBean(HlsService.class)
    public HlsService hlsService() {
        log.debug("创建 HlsService Bean");
        return new HlsService();
    }

    /**
     * 创建 MediaServer Bean
     * @param flvHandler FlvHandler 实例
     * @return MediaServer 实例
     */
    @Bean
    @ConditionalOnMissingBean(MediaServer.class)
    public MediaServer mediaServer(FlvHandler flvHandler) {
        log.debug("创建 MediaServer Bean");
        return new MediaServer(flvHandler);
    }

    /**
     * 创建 MediaEngine Bean（核心引擎）
     * @param mediaService MediaService 实例
     * @param config 媒体配置
     * @return MediaEngine 实例
     */
    @Bean
    @ConditionalOnMissingBean(MediaEngine.class)
    public MediaEngine mediaEngine(MediaService mediaService, BaseMediaConfig config) {
        log.info("创建 MediaEngine Bean...");

        // 使用 Loader 方式启动
        MediaServerLoader.load(config);

        MediaEngine engine = MediaServerLoader.getMediaEngine();
        log.info("✅ MediaEngine 初始化完成");

        return engine;
    }

    /**
     * 创建 StreamManager Bean
     * @param mediaService MediaService 实例
     * @return StreamManager 实例
     */
    @Bean
    @ConditionalOnMissingBean(StreamManager.class)
    public StreamManager streamManager(MediaService mediaService) {
        log.debug("创建 StreamManager Bean");
        return new StreamManager(mediaService);
    }

    /**
     * 创建 SpringMediaEngine Bean（高层封装，推荐使用）
     * 自动依赖注入，无需用户手动配置
     * 
     * @param mediaService MediaService 实例
     * @param config 媒体配置
     * @param streamManager StreamManager 实例
     * @return SpringMediaEngine 实例
     */
    @Bean
    @ConditionalOnMissingBean(SpringMediaEngine.class)
    public SpringMediaEngine springMediaEngine(MediaService mediaService,
                                                BaseMediaConfig config,
                                                StreamManager streamManager) {
        log.info("🚀 初始化 SpringMediaEngine...");
        
        SpringMediaEngine engine = new SpringMediaEngine();
        engine.setMediaService(mediaService);
        engine.setConfig(config);
        engine.setStreamManager(streamManager);
        
        log.info("✅ SpringMediaEngine 已就绪，可通过 @Autowired 注入使用");
        return engine;
    }
}
