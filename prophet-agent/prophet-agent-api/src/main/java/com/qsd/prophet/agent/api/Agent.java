package com.qsd.prophet.agent.api;

import com.qsd.prophet.agent.entity.LogInfo;

import java.util.List;

/**
 * Created by zhengyu on 2017/2/14.
 */
public interface Agent {
    /**
     * 获取指定日志模型的日志样例信息
     *
     * @param logInfo
     * @return
     */
    public List<String> getLogInfo(LogInfo logInfo);
}
