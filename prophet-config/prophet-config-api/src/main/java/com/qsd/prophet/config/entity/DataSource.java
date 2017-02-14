package com.qsd.prophet.config.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 定制数据源
 *
 * @author zhengyu
 */
@Data
public class DataSource implements Serializable {

    private int logModeId;
    private String id;
    private String name;
    /**
     * 1表示求和，2表示求平均
     */
    private Integer calType;
    private String calColumn;
    /**
     * 1表示需要存，2表示不需要
     */
    private Integer needRecordKeys;
    /**
     * 1表示实时查询，2表示定期归档，支持&操作
     */
    private Integer dsUseType;
    /**
     * 按顺序排好如何组装key
     */
    private String[] groupByDimNames;

    private List<DataSourceFilter> filters = new ArrayList<DataSourceFilter>();
}
