package com.ezf2jc.util;


import com.ezf2jc.dto.CameraDto;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 媒体验证器
 * 验证流地址和配置的合法性
 *
 * @author ZJ
 */
@Slf4j
public class MediaValidator {

    /**
     * 验证流地址
     */
    public static boolean validateStreamUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            log.error("流地址不能为空");
            return false;
        }

        // 检查是否为特殊地址
        if ("desktop".equalsIgnoreCase(url)) {
            return true;
        }

        // 检查是否为本地文件
        if (StreamUrlUtils.isLocalFile(url)) {
            return validateLocalFilePath(url);
        }

        // 检查是否为网络流
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            log.error("无效的流地址：{}", url);
            return false;
        }
    }

    /**
     * 验证本地文件路径
     */
    public static boolean validateLocalFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.error("文件路径不能为空");
            return false;
        }

        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            log.error("文件不存在：{}", filePath);
            return false;
        }

        if (!file.canRead()) {
            log.error("文件不可读：{}", filePath);
            return false;
        }

        return true;
    }

    /**
     * 验证 CameraDto 配置
     */
    public static boolean validateCameraDto(CameraDto cameraDto) {
        if (cameraDto == null) {
            log.error("CameraDto 不能为空");
            return false;
        }

        if (!validateStreamUrl(cameraDto.getUrl())) {
            return false;
        }

        if (cameraDto.getNetTimeout() < 1000) {
            log.error("网络超时时间不能小于 1000ms");
            return false;
        }

        if (cameraDto.getReadOrWriteTimeout() < 1000) {
            log.error("读写超时时间不能小于 1000ms");
            return false;
        }

        if (cameraDto.getNoClientsDuration() < 10000) {
            log.error("无人观看关闭时间不能小于 10000ms");
            return false;
        }

        return true;
    }

    /**
     * 验证端口号
     */
    public static boolean validatePort(int port) {
        if (port <= 0 || port > 65535) {
            log.error("端口号必须在 1-65535 之间");
            return false;
        }
        return true;
    }

    /**
     * 验证主机地址
     */
    public static boolean validateHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            log.error("主机地址不能为空");
            return false;
        }

        if ("0.0.0.0".equals(host) || "localhost".equals(host) || "127.0.0.1".equals(host)) {
            return true;
        }

        // 简单的 IP 地址验证
        String ipPattern = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";
        if (host.matches(ipPattern)) {
            String[] parts = host.split("\\.");
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    log.error("无效的 IP 地址：{}", host);
                    return false;
                }
            }
            return true;
        }

        log.error("无效的主机地址：{}", host);
        return false;
    }

    /**
     * 验证超时时间
     */
    public static boolean validateTimeout(int timeout, String name) {
        if (timeout < 1000) {
            log.error("{}不能小于 1000ms", name);
            return false;
        }
        return true;
    }
}
