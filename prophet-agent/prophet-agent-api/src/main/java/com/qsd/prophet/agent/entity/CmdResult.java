package com.qsd.prophet.agent.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class CmdResult implements Serializable {
    /**
     * 主机名
     */
    private String host;

    /**
     * 部署结果  0 成功  1失败
     */
    private Integer result = 0;

    /**
     * 脚本名称
     */
    private String scriptFile = "prophet-agent";

    /**
     * 异常描述
     */
    private String exceptionInfo = "";

    /**
     * 结果明细
     */
    private String resultDetail;
}
