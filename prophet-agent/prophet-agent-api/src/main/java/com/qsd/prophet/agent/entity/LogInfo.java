package com.qsd.prophet.agent.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class LogInfo implements Serializable {
    /**
     * 日志模型ID
     */
    private int logModeId;

    /**
     * 日志的绝对路径: key是pathID，value是对应的绝对路径信息
     */
    private Map<Integer, String> pathMap;

    /**
     * 样例日志信息采集的记录个数
     */
    private int readLogSizePerLog = 5;
}
