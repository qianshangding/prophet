package com.qsd.prophet.agent.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhengyu
 */
@Data
public class CmdInfo implements Serializable {
    /**
     * 主机IP
     */
    private String host;

    /**
     * 端口
     */
    private Integer port = 36000;

    /**
     * 用户名
     */
    private String username = "agent";

    /**
     * 密码
     */
    private String password;

    /**
     * 当前待执行脚本的名称
     */
    private String scriptFile;

    /**
     * 当前待执行脚本的MD5值
     */
    private String scriptFileMd5Sum;

    /**
     * 直接执行的命令
     */
    private String executeCommand;

    /**
     * 所在机器部署的父目录
     */
    private String deployDirectory;

    /**
     * 脚本执行的前缀
     */
    private String commandPrefix = "sh ";

    /**
     * 超时时间
     */
    private Integer timeout = 20000;
}
