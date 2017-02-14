package com.qsd.prophet.config.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志模型信息：包括日志的基本信息、日志的解析规则，日志的数据源信息
 *
 * @author zhengyu
 */
public class LogModeInfo implements Serializable {
    /**
     * 日志的维度的解析及其规则，每个类型对应的一批解析方法
     */
    private Map<Integer, List<DimensionParser>> dimensionParserMap = new HashMap<Integer, List<DimensionParser>>();

    /**
     * 日志模型计算 规则
     */
    private AppCalculateInfo appCalculateInfo;

    /**
     * 定制数据源
     */
    private List<DataSource> dataSources = new ArrayList<DataSource>();
}
