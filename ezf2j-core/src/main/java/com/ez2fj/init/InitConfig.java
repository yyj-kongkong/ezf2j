package com.ez2fj.init;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Data
public class InitConfig {
    private int port = 53251;
    private String serverName = "EZ-F2J Server";
    private String flvPath = "/live";
    private String hlsPath = "/hls";
    private String ffmpegPath = "ffmpeg";
    private String readOrWriteTimeout = "15000000";
    private String netTimeout = "15000000";
    private boolean autoClose = true;
    private long noClientsDuration = 60000;
    private ThreadPoolExecutor threadPoolExecutor;
    private boolean enableFFmpeg = false;
    private boolean enableHls = false;
    
    /**
     * 使用默认配置初始化
     */
    public InitConfig() {
        log.info("使用默认配置初始化 InitConfig");
    }
    
    /**
     * 构建者模式创建配置
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private InitConfig config = new InitConfig();
        
        public Builder port(int port) {
            config.setPort(port);
            return this;
        }
        
        public Builder serverName(String serverName) {
            config.setServerName(serverName);
            return this;
        }
        
        public Builder flvPath(String flvPath) {
            config.setFlvPath(flvPath);
            return this;
        }
        
        public Builder hlsPath(String hlsPath) {
            config.setHlsPath(hlsPath);
            return this;
        }
        
        public Builder ffmpegPath(String ffmpegPath) {
            config.setFfmpegPath(ffmpegPath);
            return this;
        }
        
        public Builder readOrWriteTimeout(String readOrWriteTimeout) {
            config.setReadOrWriteTimeout(readOrWriteTimeout);
            return this;
        }
        
        public Builder netTimeout(String netTimeout) {
            config.setNetTimeout(netTimeout);
            return this;
        }
        
        public Builder autoClose(boolean autoClose) {
            config.setAutoClose(autoClose);
            return this;
        }
        
        public Builder noClientsDuration(long noClientsDuration) {
            config.setNoClientsDuration(noClientsDuration);
            return this;
        }
        
        public Builder threadPoolExecutor(ThreadPoolExecutor executor) {
            config.setThreadPoolExecutor(executor);
            return this;
        }
        
        public Builder enableFFmpeg(boolean enableFFmpeg) {
            config.setEnableFFmpeg(enableFFmpeg);
            return this;
        }
        
        public Builder enableHls(boolean enableHls) {
            config.setEnableHls(enableHls);
            return this;
        }
        
        public InitConfig build() {
            return config;
        }
    }
}

