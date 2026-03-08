package com.ezf2jc.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 相机 DTO
 * 用于传输相机配置信息
 *
 * @author ZJ
 */
@Getter
@Setter
public class CameraDto implements Serializable {

    private static final long serialVersionUID = -5575352151805386129L;

    /**
     * 视频流地址（rtsp/rtmp/本地文件路径/desktop）
     */
    private String url;

    /**
     * 流备注
     */
    private String remark;

    /**
     * FLV 开启状态
     */
    private boolean enabledFlv = true;

    /**
     * HLS 开启状态
     */
    private boolean enabledHls = false;

    /**
     * 是否启用 FFmpeg（true=FFmpeg，false=JavaCV）
     */
    private boolean enabledFFmpeg = false;

    /**
     * 无人拉流观看是否自动关闭流
     */
    private boolean autoClose = true;

    /**
     * MD5 key，媒体标识，区分不同媒体
     */
    private String mediaKey;

    /**
     * 网络超时时间（微秒），默认 15 秒
     */
    private long netTimeout = -1;

    /**
     * 读写超时时间（微秒），默认 15 秒
     */
    private long readOrWriteTimeout = -1;

    /**
     * 无人拉流观看持续多久自动关闭（毫秒），默认 60 秒
     */
    private long noClientsDuration = 0;

    /**
     * 流类型：0=网络流，1=本地视频
     */
    private int type = 0;

    /**
     * 流标识
     */
    private String streamId;
}
