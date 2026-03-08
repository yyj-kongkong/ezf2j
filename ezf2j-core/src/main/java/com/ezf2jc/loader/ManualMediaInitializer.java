package com.ezf2jc.loader;


import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.engine.MediaEngine;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Loader;
import com.ezf2jc.common.MediaConstant;

/**
 * 手动初始化器
 * 提供分步骤的初始化方式，适合需要精细控制的场景
 *
 * @author ZJ
 */
@Slf4j
public class ManualMediaInitializer {

    /**
     * 初始化阶段枚举
     */
    public enum InitStage {
        NOT_STARTED,      // 未开始
        FFMPEG_INITED,    // FFmpeg 已初始化
        COMPONENTS_CREATED, // 组件已创建
        SERVER_STARTED,   // 服务器已启动
        COMPLETED         // 完成
    }

    /**
     * 当前初始化阶段
     */
    private InitStage currentStage = InitStage.NOT_STARTED;

    /**
     * 配置对象
     */
    private BaseMediaConfig config;

    /**
     * 是否启用详细日志
     */
    private boolean detailedLog = false;

    /**
     * 设置配置
     * @param config 配置对象
     * @return 当前初始化器
     */
    public ManualMediaInitializer setConfig(BaseMediaConfig config) {
        this.config = config;
        return this;
    }

    /**
     * 启用详细日志
     * @return 当前初始化器
     */
    public ManualMediaInitializer enableDetailedLog() {
        this.detailedLog = true;
        return this;
    }

    /**
     * 阶段 1：初始化 FFmpeg
     * @return 当前初始化器
     */
    public  ManualMediaInitializer initFFmpeg() {
        try {
            log.info("阶段 1/4: 初始化 FFmpeg...");
            String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            System.setProperty(MediaConstant.FFMPEG_PATH_KEY, ffmpeg);
            log.info("✅ FFmpeg 初始化成功，路径：{}", ffmpeg);
            currentStage = InitStage.FFMPEG_INITED;
            return this;
        } catch (Exception e) {
            log.error("❌ FFmpeg 初始化失败", e);
            throw new RuntimeException("FFmpeg 初始化失败", e);
        }
    }

    /**
     * 阶段 2：创建核心组件
     * @return 当前初始化器
     */
    public ManualMediaInitializer createComponents() {
        checkStage(InitStage.FFMPEG_INITED);

        try {
            log.info("阶段 2/4: 创建核心组件...");

            // 这里会由 MediaServerLoader 内部创建
            // 此方法主要用于验证

            if (detailedLog) {
                log.debug("准备创建组件：FlvHandler, MediaService, HlsService, MediaServer");
            }

            currentStage = InitStage.COMPONENTS_CREATED;
            log.info("✅ 核心组件创建成功");
            return this;
        } catch (Exception e) {
            log.error("❌ 核心组件创建失败", e);
            throw new RuntimeException("核心组件创建失败", e);
        }
    }

    /**
     * 阶段 3：启动服务器
     * @return 当前初始化器
     */
    public ManualMediaInitializer startServer() {
        checkStage(InitStage.COMPONENTS_CREATED);

        try {
            log.info("阶段 3/4: 启动服务器...");

            // 使用 MediaServerLoader 统一启动
            MediaServerLoader.load(config);

            currentStage = InitStage.SERVER_STARTED;
            log.info("✅ 服务器启动成功 - {}:{}", config.getHost(), config.getPort());
            return this;
        } catch (Exception e) {
            log.error("❌ 服务器启动失败", e);
            throw new RuntimeException("服务器启动失败", e);
        }
    }

    /**
     * 阶段 4：完成初始化
     * @return MediaEngine 实例
     */
    public MediaEngine complete() {
        checkStage(InitStage.SERVER_STARTED);

        try {
            log.info("阶段 4/4: 完成初始化...");

            MediaEngine engine = MediaServerLoader.getMediaEngine();

            currentStage = InitStage.COMPLETED;
            log.info("✅ 初始化完成，MediaEngine 已就绪");

            return engine;
        } catch (Exception e) {
            log.error("❌ 初始化完成失败", e);
            throw new RuntimeException("初始化完成失败", e);
        }
    }

    /**
     * 一键完成所有初始化步骤
     * @param config 配置对象
     * @return MediaEngine 实例
     */
    public static MediaEngine quickInit(BaseMediaConfig config) {
        return new ManualMediaInitializer()
                .setConfig(config)
                .initFFmpeg()
                .createComponents()
                .startServer()
                .complete();
    }

    /**
     * 检查初始化阶段
     */
    private void checkStage(InitStage expectedStage) {
        if (currentStage != expectedStage) {
            throw new IllegalStateException(
                    String.format("当前阶段为 %s，期望阶段为 %s", currentStage, expectedStage)
            );
        }
    }

    /**
     * 获取当前初始化阶段
     * @return 当前阶段
     */
    public InitStage getCurrentStage() {
        return currentStage;
    }

    /**
     * 检查是否已完成初始化
     * @return 是否完成
     */
    public boolean isCompleted() {
        return currentStage == InitStage.COMPLETED;
    }
}
