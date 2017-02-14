package com.qsd.prophet.agent.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeployInfo implements Serializable {
    /**
     * 主机名
     */
    private String host;

    /**
     * 端口
     */
    private Integer port = 22;

    /**
     * 用户名
     */
    private String username = "agent";

    /**
     * 密码
     */
    private String password;

    /**
     * 待部署应用名称
     */
    private String appName = "polaris-agent";

    /**
     * 超时时间
     */
    private Integer timeout = 20000;

    /**
     * 部署类型 1---安装，2---升级，3---回滚，4---停止，5---重启
     */
    private Integer deployType = 1;

    /**
     * 部署操作前，当前主机所部属的软件类型
     */
    private String lastSoftPacakge;

    /**
     * 前一次已部署软件包MD5值
     */
    private String lastSoftPacakgeMd5Sum;

    /**
     * 当前待部署软件包名
     */
    private String currentSoftPacakge;

    /**
     * 当前待部署软件包MD5值
     */
    private String currentSoftPacakgeMd5Sum;

    /**
     * 安装包的压缩类型  1--zip, 2---.gz , 3---tar.gz
     */
    private Integer compressType = 1;

    /**
     * 解压后的名称
     */
    private String decompressDirName;

    /**
     * 所在机器部署的父目录
     */
    private String deployDirectory;

    /**
     * 启动文件的路径，相对所在机器部署的父目录的相对路径
     */
    private String startUpFilePath;

    /**
     * 停止文件的路径，相对软件安装父目录的路径
     */
    private String stopFilePath;

    /**
     * 健康检查文件的路径，相对软件安装父目录的的路径
     */
    private String healthCheckFilePath;

    /**
     * 监控文件的路径，相对软件安装父目录的的路径
     */
    private String monitorFilePath;

    /**
     * 删除crontab文件的路径，相对软件安装父目录的的路径
     */
    private String rmCrontabFilePath;

}
