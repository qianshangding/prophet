package com.qsd.prophet.agent.service.entity;

import lombok.Data;

import java.util.List;

@Data
public class AgentHeartBeatInfo {
    /**
     * 心跳上报的主机名
     */
    private String hostName;

    /**
     * 心跳上报的时间
     */
    private String uploadHeartBeatTime;

    /**
     * 心跳的详细信息
     */
    private List<FileCollcetHeartbeatInfo> fileCollcetHeartbeatInfos;
}
