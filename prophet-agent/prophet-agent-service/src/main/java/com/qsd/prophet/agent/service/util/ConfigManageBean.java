package com.qsd.prophet.agent.service.util;

/**
 * 属性注入类
 *
 * @author zhengyu
 */
public class ConfigManageBean {

    private Integer mqbatchSize;

    public ConfigManageBean() {
        super();
    }

    public Integer getMqbatchSize() {
        return mqbatchSize;
    }

    public void setMqbatchSize(Integer mqbatchSize) {
        this.mqbatchSize = mqbatchSize;
    }

}
