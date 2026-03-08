package com.ezf2jc.common;


/**
 * 流状态枚举
 *
 * @author ZJ
 */
public enum StreamStatus {

    /**
     * 初始化中
     */
    INITIALIZING(0, "初始化中"),

    /**
     * 运行中
     */
    RUNNING(1, "运行中"),

    /**
     * 暂停
     */
    PAUSED(2, "暂停"),

    /**
     * 停止
     */
    STOPPED(3, "停止"),

    /**
     * 错误
     */
    ERROR(4, "错误"),

    /**
     * 等待连接
     */
    WAITING(5, "等待连接"),

    /**
     * 已断开
     */
    DISCONNECTED(6, "已断开");

    private final int code;
    private final String description;

    StreamStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据状态码获取枚举
     * @param code 状态码
     * @return StreamStatus
     */
    public static StreamStatus valueOf(int code) {
        for (StreamStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的流状态：" + code);
    }

    /**
     * 是否正在运行
     * @return 是否运行中
     */
    public boolean isRunning() {
        return this == RUNNING;
    }

    /**
     * 是否已停止
     * @return 是否已停止
     */
    public boolean isStopped() {
        return this == STOPPED || this == DISCONNECTED;
    }

    /**
     * 是否有错误
     * @return 是否有错误
     */
    public boolean hasError() {
        return this == ERROR;
    }
}
