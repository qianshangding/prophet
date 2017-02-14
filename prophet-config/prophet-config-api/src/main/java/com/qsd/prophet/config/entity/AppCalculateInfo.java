package com.qsd.prophet.config.entity;

import com.qsd.prophet.commons.constant.LogType;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppCalculateInfo implements Serializable {
    private int logModeId;
    private LogType logType;
    private String toApp;
    private String interfaze;
    private String method;
    private String result;
    private String count;
    private String cost;
}
