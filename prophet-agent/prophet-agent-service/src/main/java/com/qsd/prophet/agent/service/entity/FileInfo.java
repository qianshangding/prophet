package com.qsd.prophet.agent.service.entity;

import com.qsd.prophet.commons.util.TimeUtil;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Date;

/**
 * 采集日志文件元数据信息
 *
 * @author zhengyu
 */
@Data
public class FileInfo {

    /**
     * 日志模型ID
     */
    private Integer logID;

    /**
     * 日志路径ID标示
     */
    private Integer pathID;
    /**
     * 所属应用
     */
    private String appName;

    private String projectName;

    /**
     * 文件的父目录
     */
    private String parentPath;

    /**
     * 文件名
     */
    private String logName;

    /**
     * 文件采集的当前行数
     */
    private Long offSet;
    /**
     * 日志采集类型,1、常规的周期性采集  2、临时性采集
     */
    private Integer collectType;
    /**
     * 编码类型
     */
    private String encodeType;

    /**
     * 日志用途
     */
    private Integer useType;
    /**
     * 日志传输类型
     */
    private Integer transType;
    /**
     * 传输标签
     */
    private String transName;

    /**
     * 日志内容中时间的格式
     */
    private String timeFormat = TimeUtil.YYYY_MM_DD_HH_MM_SS;

    /**
     * 日志内容开始标示
     */
    private String timeStartFlag = "";

    /**
     * 第几个开始标示
     */
    private Integer timeStartFlagIndex = 0;
    /**
     * 日志采集开始时刻
     */
    private Long startTime;
    /**
     * 日志采集停止时刻
     */
    private Long endTime;

    /**
     * 是否最新的文件
     */
    private Boolean isLatestFile = true;

    /**
     * 滚动之后文件的最新修改时间是固定的
     */
    private Long lastModifyTime = null;

    public String getAbsFilePath() {
        return parentPath + File.separator + logName;
    }

    public String getLogIDAndPathIDKey() {
        return logID + "_" + pathID;
    }

    public FileInfo getRefreshFileInfo() {

        FileInfo fileInfo = new FileInfo();
        fileInfo.setAppName(appName);
        fileInfo.setProjectName(projectName);
        fileInfo.setCollectType(collectType);
        fileInfo.setEncodeType(encodeType);
        fileInfo.setLogID(logID);
        fileInfo.setPathID(pathID);
        fileInfo.setLogName(logName);
        fileInfo.setParentPath(parentPath);
        fileInfo.setTimeStartFlag(timeStartFlag);
        fileInfo.setTimeStartFlagIndex(timeStartFlagIndex);
        fileInfo.setTransName(transName);
        fileInfo.setTimeFormat(timeFormat);
        fileInfo.setTransType(transType);
        fileInfo.setUseType(useType);

        return fileInfo;
    }

    public LogEvent getCommonLogEvent() {

        LogEvent logEvent = new LogEvent();
        logEvent.setParentPath(parentPath);
        logEvent.setLogName(logName);
        logEvent.setAppName(appName);
        logEvent.setLogID(logID);
        logEvent.setPathID(pathID);
        if (StringUtils.isEmpty(transName)) {
            logEvent.setLogName(logName);
        } else {
            logEvent.setLogName(transName);
        }
        logEvent.setTransType(transType);
        logEvent.setUseType(useType);
        logEvent.setProjectName(projectName);
        logEvent.setTransName(transName);

        return logEvent;
    }
}
