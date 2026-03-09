package com.ez2fj.common;

import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 常量配置
 * @author ZJ
 *
 */
public class EZF2JConfig {

    public static String NET_TIMEOUT = "15000000";
    public static long NO_CLIENTS_DURATION = 60000;
    //header server 名称
	public static String serverName = "serverName";
	//自定义链式线程池
	public static ThreadPoolExecutor threadpool = new ThreadPoolExecutor(20, 500, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadPoolExecutor.CallerRunsPolicy());
	public static String ffmpegPathKey = "ffmpegPathKey";
	public static int PORT = 53251;
	public static boolean AUTO_CLOSE = true;
    public static String READ_OR_WRITE_TIMEOUT = "15000000";
    public static String FLV_PATH = "/live";
    public static String HLS_PATH = "/hls";
    public static boolean ENABLE_FFMPEG = false;
    public static boolean ENABLE_HLS = false;
    public static Map<String, String> getConfig(){
        return Map.of(
            "port", String.valueOf(PORT),
            "serverName", serverName,
            "ffmpegPathKey", ffmpegPathKey,
            "autoClose", String.valueOf(AUTO_CLOSE),
            "readOrWriteTimeout", READ_OR_WRITE_TIMEOUT,
            "flvPath", FLV_PATH,
            "hlsPath", HLS_PATH,
            "enableFFmpeg", String.valueOf(ENABLE_FFMPEG),
            "enableHls", String.valueOf(ENABLE_HLS)
        );
    };

}
