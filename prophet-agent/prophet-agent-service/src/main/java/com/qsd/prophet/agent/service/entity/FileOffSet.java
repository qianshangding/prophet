package com.qsd.prophet.agent.service.entity;

import lombok.Data;

/**
 * 日志采集位置记录信息
 *
 * @author zhengyu
 */
@Data
public class FileOffSet {
    private Integer logID;
    private Integer pathID;
    private Long offSet;
    private Long timeStamp;
    private String parentPath;
    private String fileName;
    private Long lastModifyTime;
}
