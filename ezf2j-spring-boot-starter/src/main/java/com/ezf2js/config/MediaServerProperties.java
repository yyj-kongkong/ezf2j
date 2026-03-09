package com.ezf2js.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ezf2j.media")
public class MediaServerProperties {

    private Boolean enabled = true;

    private Integer port = 53251;

    private String serverName = "EZ-F2J Media Server";

    private String flvPath = "/live";

    private String hlsPath = "/hls";

    private String ffmpegPath = "ffmpeg";

    private String readOrWriteTimeout = "15000000";

    private String netTimeout = "15000000";

    private boolean autoClose = true;

    private long noClientsDuration = 60000L;

    private boolean enableFFmpeg = false;

    private boolean enableHls = false;

    private Integer threadPoolCoreSize;

    private Integer threadPoolMaxSize;
}
