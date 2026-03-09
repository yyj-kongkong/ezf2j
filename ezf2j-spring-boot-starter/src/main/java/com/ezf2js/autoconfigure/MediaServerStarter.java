package com.ezf2js.autoconfigure;

import com.ez2fj.EZF2JEngine;
import com.ez2fj.init.InitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MediaServerStarter implements CommandLineRunner {

    @Autowired(required = false)
    private InitConfig initConfig;

    @Override
    public void run(String... args) throws Exception {
        if (initConfig != null) {
            log.info("🎯 使用配置启动 EZF2J 服务...");
            EZF2JEngine.init(initConfig);
        } else {
            log.info("🎯 使用默认配置启动 EZF2J 服务...");
            EZF2JEngine.getInstance();
        }
        log.info("✅ EZF2J 媒体服务器已启动");
    }
}
