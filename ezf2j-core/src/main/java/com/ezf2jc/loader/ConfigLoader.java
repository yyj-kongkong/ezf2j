package com.ezf2jc.loader;


import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.config.CustomizableMediaConfig;
import com.ezf2jc.engine.MediaEngine;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Properties;

/**
 * 配置文件加载器
 * 从 properties 文件读取配置
 *
 * @author ZJ
 */
@Slf4j
public class ConfigLoader {

    /**
     * 从类路径加载配置文件
     * @param configPath 配置文件路径（如：media-server.properties）
     * @return 配置对象
     */
    public static BaseMediaConfig loadFromClasspath(String configPath) {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(configPath)) {
            if (input == null) {
                log.error("无法找到配置文件：{}", configPath);
                throw new RuntimeException("配置文件不存在：" + configPath);
            }
            return loadFromStream(input);
        } catch (IOException e) {
            log.error("读取配置文件失败", e);
            throw new RuntimeException("读取配置文件失败", e);
        }
    }

    /**
     * 从文件路径加载配置文件
     * @param filePath 文件路径
     * @return 配置对象
     */
    public static BaseMediaConfig loadFromFile(String filePath) {
        try (InputStream input = new FileInputStream(filePath)) {
            return loadFromStream(input);
        } catch (IOException e) {
            log.error("读取配置文件失败", e);
            throw new RuntimeException("读取配置文件失败", e);
        }
    }

    /**
     * 从输入流加载配置
     * @param input 输入流
     * @return 配置对象
     */
    public static BaseMediaConfig loadFromStream(InputStream input) {
        try {
            Properties props = new Properties();
            props.load(input);
            return buildConfig(props);
        } catch (IOException e) {
            log.error("解析配置文件失败", e);
            throw new RuntimeException("解析配置文件失败", e);
        }
    }

    /**
     * 从 Properties 构建配置
     * @param props Properties 对象
     * @return 配置对象
     */
    public static BaseMediaConfig buildConfig(Properties props) {
        CustomizableMediaConfig config = new CustomizableMediaConfig();

        // 基本配置
        config.setPort(getIntProperty(props, "media.server.port", 8866));
        config.setHost(getStringProperty(props, "media.server.host", "0.0.0.0"));
        config.setNetTimeout(getIntProperty(props, "media.server.net-timeout", 15000));
        config.setReadOrWriteTimeout(getIntProperty(props, "media.server.read-or-write-timeout", 15000));
        config.setAutoClose(getBooleanProperty(props, "media.server.auto-close", true));
        config.setNoClientsDuration(getLongProperty(props, "media.server.no-clients-duration", 60000L));
        config.setEnableFFmpeg(getBooleanProperty(props, "media.server.enable-ffmpeg", false));
        config.setFfmpegPath(getStringProperty(props, "media.server.ffmpeg-path", null));
        config.setLogLevel(getIntProperty(props, "media.server.log-level", 3));

        // HLS 配置
        BaseMediaConfig.HlsConfig hlsConfig = config.getHlsConfig();
        hlsConfig.setHost(getStringProperty(props, "media.server.hls.host", "localhost"));
        hlsConfig.setPort(getIntProperty(props, "media.server.hls.port", 8866));
        hlsConfig.setTime(getIntProperty(props, "media.server.hls.time", 1));
        hlsConfig.setWrap(getIntProperty(props, "media.server.hls.wrap", 6));
        hlsConfig.setListSize(getIntProperty(props, "media.server.hls.list-size", 1));

        log.info("已从配置文件加载配置：端口={}, 地址={}", config.getPort(), config.getHost());

        return config;
    }

    /**
     * 获取字符串属性
     */
    private static String getStringProperty(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        return value != null ? value.trim() : defaultValue;
    }

    /**
     * 获取整数属性
     */
    private static int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("属性 {} 值无效：{}, 使用默认值：{}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取长整型属性
     */
    private static long getLongProperty(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                log.warn("属性 {} 值无效：{}, 使用默认值：{}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取布尔属性
     */
    private static boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    /**
     * 快速加载并启动（从类路径）
     * @param configPath 配置文件路径
     * @return MediaEngine 实例
     */
    public static MediaEngine quickLoadFromClasspath(String configPath) {
        BaseMediaConfig config = loadFromClasspath(configPath);
        return MediaServerLoader.load(config);
    }

    /**
     * 快速加载并启动（从文件）
     * @param filePath 文件路径
     * @return MediaEngine 实例
     */
    public static MediaEngine quickLoadFromFile(String filePath) {
        BaseMediaConfig config = loadFromFile(filePath);
        return MediaServerLoader.load(config);
    }
}
