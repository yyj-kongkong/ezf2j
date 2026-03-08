package com.ezf2jc.thread;

/**
 * 转码回调接口
 * 用于通知转码启动状态
 *
 * @author ZJ
 */
public interface TransferCallback {

    /**
     * 转码启动回调
     * @param startSuccess 是否启动成功
     */
    void start(boolean startSuccess);
}
