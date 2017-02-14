package com.qsd.prophet.config.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 定制数据源的条件过滤器
 *
 * @author zhengyu
 */
@Data
public class DataSourceFilter implements Serializable {

    /**
     * 过滤的字段既可以是维度也可以是数值
     */
    private String name;
    /**
     * opr_type 取值类型, 1:=, 2:>, 3:<, 4:in, 5:>=, 6:<=, 7:!=
     */
    private int oprType;
    private String filterValue;
}
