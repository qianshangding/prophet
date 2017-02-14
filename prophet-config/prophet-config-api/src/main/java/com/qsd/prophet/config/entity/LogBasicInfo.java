package com.qsd.prophet.config.entity;

import com.qsd.prophet.commons.util.TimeUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class LogBasicInfo implements Serializable {

    private static final long serialVersionUID = 7402906815993280878L;

    /**
     * 日志模型ID
     */
    private Integer logModeId;

    /**
     * 日志模型名称
     */
    private String logModeName;

    /**
     * 日志模型类型，1、servcie  2、sal  4、dal 8、error 16、custom data source  支持组合，比如即是service又有自定义数据源，对应的log_type为17
     */
    private Integer logType;

    /**
     * 日志的绝对路径: key是pathID，value是对应的绝对路径信息
     */
    private Map<Integer, String> pathMap;

    /**
     * 所属应用
     */
    private String belongToCluster = "default";

    /**
     * 所对应的主机列表信息
     */
    private List<String> hosts = new ArrayList<String>();

    /**
     * 应用来源
     */
    private String queryFrom = "kuaidi";

    /**
     * 日志采集类型,1、周期性采集  2、定时采集
     */
    private Integer collectType = 1;
    /**
     * 编码类型
     */
    private String encodeType = "UTF-8";

    /**
     * 日志用途
     * use_type的含义：
     * 1、表示用于大数据分析
     * 2、表示用于日志检索
     * 4、表示用于Storm的流失计算
     * 8、用于异常日志分析
     */
    private Integer useType = 1;
    /**
     * 日志传输类型 trans_type的含义：
     * 1、flume采集
     * 2、mq采集
     */
    private Integer transType = 1;
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
    private int timeStartFlagIndex = 0;
    /**
     * 日志采集开始时刻
     */
    private Date startTime;
    /**
     * 日志采集停止时刻
     */
    private Date endTime;
}
