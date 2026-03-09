package com.ezf2js.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EngineAdapter {

    @Autowired(required = false)
    private SpringMediaEngine springEngine;

    public boolean isSpringEnvironment() {
        return springEngine != null;
    }

    public String startStream(String streamUrl) {
        if (isSpringEnvironment()) {
            log.debug("使用 Spring 环境推流");
            return springEngine.startStream(streamUrl);
        } else {
            log.warn("非 Spring 环境，无法推流");
            throw new IllegalStateException("EZF2J 未初始化");
        }
    }

    public String startStream(String streamUrl, boolean useFFmpeg) {
        if (isSpringEnvironment()) {
            log.debug("使用 Spring 环境推流 (FFmpeg:{})", useFFmpeg);
            return springEngine.startStream(streamUrl, useFFmpeg);
        } else {
            log.warn("非 Spring 环境，无法推流");
            throw new IllegalStateException("EZF2J 未初始化");
        }
    }

    public void stopStream(String streamUrl) {
        if (isSpringEnvironment()) {
            log.debug("使用 Spring 环境关闭推流");
            springEngine.stopStream(streamUrl);
        } else {
            log.warn("非 Spring 环境，无法关闭推流");
            throw new IllegalStateException("EZF2J 未初始化");
        }
    }

    public String getWsPlayUrl(String streamUrl) {
        if (isSpringEnvironment()) {
            return springEngine.getWsPlayUrl(streamUrl);
        } else {
            throw new IllegalStateException("EZF2J 未初始化");
        }
    }

    public String getHttpPlayUrl(String streamUrl) {
        if (isSpringEnvironment()) {
            return springEngine.getHttpPlayUrl(streamUrl);
        } else {
            throw new IllegalStateException("EZF2J 未初始化");
        }
    }

    public String getHlsPlayUrl(String streamUrl) {
        if (isSpringEnvironment()) {
            return springEngine.getHlsPlayUrl(streamUrl);
        } else {
            throw new IllegalStateException("EZF2J 未初始化");
        }
    }

    public int getActiveStreamCount() {
        if (isSpringEnvironment()) {
            return springEngine.getCameraCount();
        } else {
            return 0;
        }
    }

    public SpringMediaEngine getSpringEngine() {
        return springEngine;
    }
}
