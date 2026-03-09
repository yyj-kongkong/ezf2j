package com.ezf2jc;

import com.ezf2jc.config.CustomizableMediaConfig;
import com.ezf2jc.engine.MediaEngine;
import com.ezf2jc.loader.ManualMediaInitializer;
import com.ezf2jc.util.FFmpegUtils;

public class test {
    public static void main(String[] args) {
        CustomizableMediaConfig customizableMediaConfig = new CustomizableMediaConfig();
        customizableMediaConfig.setPort(8866);
        customizableMediaConfig.setEnableFFmpeg( true);
        customizableMediaConfig.setLogLevel(3);
        MediaEngine engine = new ManualMediaInitializer()
                .setConfig(customizableMediaConfig)
                .initFFmpeg()
                .createComponents()
                .startServer()
                .complete();

//        String fFmpegVersion = FFmpegUtils.getFFmpegVersion();
//        System.out.println("FFmpeg版本：" + fFmpegVersion);
//        String s = engine.startStream("tcp://127.0.0.1:123");
//        System.out.println("推流地址：" + s);
//        engine.getHttpPlayUrl("tcp://127.0.0.1:123", "127.0.0.1", 8866);
    }
}
