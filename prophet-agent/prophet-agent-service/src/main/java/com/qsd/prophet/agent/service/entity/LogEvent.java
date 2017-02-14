package com.qsd.prophet.agent.service.entity;

import lombok.Data;

/**
 * 传输的日志元数据信息
 *
 * @author zhengyu
 */
@Data
public class LogEvent {

    /**
     * 日志模型ID
     */
    private Integer logID;

    /**
     * 日志路径ID标示
     */
    private Integer pathID;

    /**
     * 文件的父目录
     */
    private String parentPath;

    /**
     * 文件名
     */
    private String logName;

    /**
     * 文件最后一次更新的时间 ，对于不是主文件，这个字段都不为空
     */
    private Long lastModifyTime;

    /**
     * 所属应用
     */
    private String appName;

    /**
     * 应用所属的项目名称
     */
    private String projectName;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 日志所在的行数
     */
    private Long offSet;
    /**
     * 日志用途
     */
    private Integer useType = 1;
    /**
     * 日志传输类型
     */
    private Integer transType;

    /**
     * 传输标签
     */
    private String transName;

    /**
     * 日志生成的时间
     */
    private Long createTimeStamp;

    private String createTimeString;

    /**
     * 所在主机名
     */
    private String hostName;
}
