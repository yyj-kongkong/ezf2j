package com.ez2fj.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.digest.MD5;
import com.ez2fj.common.CacheMap;
import com.ez2fj.model.Camera;
import com.ez2fj.thread.MediaTransferHls;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

public class HlsService {
    private static volatile HlsService instance;
    
    private HlsService() {}
    
    public static HlsService getInstance() {
        if (instance == null) {
            synchronized (HlsService.class) {
                if (instance == null) {
                    instance = new HlsService();
                }
            }
        }
        return instance;
    }
	
	/**
	 * 
	 */
	public static ConcurrentHashMap<String, MediaTransferHls> cameras = new ConcurrentHashMap<>(); 
	
	/**
	 * 定义ts缓存10秒
	 */
	public static CacheMap<String, byte[]> cacheTs = new CacheMap<>(10000);
	public static CacheMap<String, byte[]> cacheM3u8 = new CacheMap<>(10000);
	
	/**
	 * 保存ts
	 * @param
	 */
	public void processTs(String mediaKey, String tsName, InputStream in) {
		byte[] readBytes = IoUtil.readBytes(in);
		String tsKey = mediaKey.concat("-").concat(tsName);
		cacheTs.put(tsKey, readBytes);
	}

	/**
	 * 保存hls
	 * @param mediaKey
	 * @param in
	 */
	public void processHls(String mediaKey, InputStream in) {
		byte[] readBytes = IoUtil.readBytes(in);
		cacheM3u8.put(mediaKey, readBytes);
	}

	/**
	 * 关闭hls切片
	 * 
	 * @param camera
	 */
	public void closeConvertToHls(Camera camera) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());

		if (cameras.containsKey(mediaKey)) {
			MediaTransferHls mediaTransferHls = cameras.get(mediaKey);
			mediaTransferHls.stop();
			cameras.remove(mediaKey);
			cacheTs.remove(mediaKey);
			cacheM3u8.remove(mediaKey);
		}
	}

	/**
	 * 开始hls切片
	 * 
	 * @param camera
	 * @return
	 */
	public boolean startConvertToHls(Camera camera) {

		// 区分不同媒体
		String mediaKey = MD5.create().digestHex(camera.getUrl());
		camera.setMediaKey(mediaKey);

		MediaTransferHls mediaTransferHls = cameras.get(mediaKey);

		if (null == mediaTransferHls) {
			mediaTransferHls = new MediaTransferHls(camera, Convert.toInt("port"));
			cameras.put(mediaKey, mediaTransferHls);
			mediaTransferHls.execute();
		}

		mediaTransferHls = cameras.get(mediaKey);
		
		// 15秒还没true认为启动不了
		for (int i = 0; i < 30; i++) {
			if (mediaTransferHls.isRunning()) {
				return true;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		return false;
	}

}
