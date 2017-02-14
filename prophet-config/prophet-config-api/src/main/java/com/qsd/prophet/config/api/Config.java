package com.qsd.prophet.config.api;

import com.qsd.prophet.config.entity.LogBasicInfo;
import com.qsd.prophet.config.entity.LogModeInfo;

import java.util.List;
import java.util.Map;

/**
 * 提供RPC调用的接口
 *
 * @author zhengyu
 */
public interface Config {

    /**
     * 获取所有日志模型相关信息，key是log_mode_id
     *
     * @return
     */
    Map<Integer, LogModeInfo> getLogModeInfoList();

    /**
     * 根据主机名获取对应的日志模型信息列表
     *
     * @param hostName
     * @return
     */
    List<LogBasicInfo> getLogInfoByHost(String hostName);

    /**
     * 更新日志模型的采集状态目前主要是采集结束
     *
     * @param hostName 日志模型ID
     * @param pathID   PATH ID
     * @param status   1 停止采集
     * @return
     */
    boolean updateLogCollectStatus(String hostName, Integer pathID, Integer status);
}
