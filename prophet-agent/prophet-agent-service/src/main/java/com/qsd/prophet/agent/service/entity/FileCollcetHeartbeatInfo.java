package com.qsd.prophet.agent.service.entity;

import lombok.Data;

@Data
public class FileCollcetHeartbeatInfo {
    /**
     * 日志模型ID
     */
    private int logID;

    /**
     * 日志路径ID标示
     */
    private Integer pathID;

    /**
     * 文件名
     */
    private String logName;

    /**
     * 当前采集到的时间戳
     */
    private String currentCollectTimestamp;

    /**
     * 当前采集的个数
     */
    private Integer currentCollectCount;

    private Long heartBeatTime;

    public void incrCurrentCollectCount() {
        currentCollectCount += 1;
    }

    public void reset() {
        currentCollectCount = 0;
        currentCollectTimestamp = null;
    }
}
