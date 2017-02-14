package com.qsd.prophet.config.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 包含日志分割信息和维度词典映射
 *
 * @author zhengyu
 */
@Data
public class DimensionParser implements Serializable {
    private int id;
    private int logModeId;
    /**
     * 表示不同解析的类型，一个日志模型可以有多个解析类型
     */
    private int parseCategoryId;
    private String name;
    /**
     * 维度列还是数值列, 1是维度，2是数值
     */
    private int type;
    private String startFlag;
    /**
     * 第几个起始的字符开始
     */
    private int startFlagIndex;
    private String endFlag;
    /**
     * 维度映射字典，数值列则为null
     */
    private Map<String, String> dimDictionary;
    /**
     * 对start和end截取出来的字符串应用的正则表达式
     */
    private String regularExpression;
    /**
     * 编译好的正则Pattern
     */
    private Pattern pattern;
    /**
     * 对start和end截取出来的字符串应用的过滤器，不能包含以下数组里任意一个String
     */
    private String[] dimensionFilter;
}
