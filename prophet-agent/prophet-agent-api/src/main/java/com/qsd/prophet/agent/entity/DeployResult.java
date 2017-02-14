package com.qsd.prophet.agent.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeployResult implements Serializable {
    /**
     * 主机名
     */
    private String host;

    /**
     * 部署结果  0 成功  1失败
     */
    private Integer result = 0;

    private String appName = "prophet-agent";

    private String exceptionInfo;
}