package com.ezf2js.autoconfigure;

import com.ezf2jc.config.BaseMediaConfig;
import com.ezf2jc.engine.MediaEngine;
import com.ezf2jc.loader.MediaServerLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bean 注册器
 * 手动注册必要的 Bean（用于特殊场景）
 *
 * @author ZJ
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ezf2j.media", name = "manual-register", havingValue = "true")
public class MediaServerBeanRegistrar implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.info("手动注册媒体服务器 Bean...");

        // 如果已经加载，直接返回
        if (MediaServerLoader.isRunning()) {
            log.debug("媒体服务器已经运行，跳过注册");
            return;
        }

        // 获取配置
        BaseMediaConfig config = beanFactory.getBean(BaseMediaConfig.class);
        if (config == null) {
            log.warn("未找到 BaseMediaConfig Bean，使用默认配置");
            config = new com.ezf2jc.config.DefaultMediaConfig();
        }

        // 启动服务
        try {
            MediaServerLoader.load(config);
            log.info("✅ 媒体服务器 Bean 注册完成");
        } catch (Exception e) {
            log.error("❌ 媒体服务器 Bean 注册失败", e);
        }
    }
}
