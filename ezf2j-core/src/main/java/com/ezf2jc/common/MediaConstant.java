package com.ezf2jc.common;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 常量配置
 *
 */
public class MediaConstant {

    public static final String SERVER_NAME =  "EZF2J";
    public static final String FFMPEG_PATH_KEY =  "EZF2J";
	//自定义链式线程池
	public static ThreadPoolExecutor threadpool = new ThreadPoolExecutor(20, 500, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadPoolExecutor.CallerRunsPolicy());

}
