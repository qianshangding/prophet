package com.qsd.prophet.agent.service.common;

/**
 * Created by zhengyu on 2017/2/14.
 */
public interface AgentConstant {
    String SEPARATORCHAR                   = "###";
    //日志处理的对象
    int    LOG_FOR_HADOOP                  = 1;
    int    LOG_FOR_SEARCH                  = 2;
    int    LOG_FOR_JSTORM                  = 4;

    //数据来源
    int    FLUME_TRANS_TYPE                = 1;
    int    MQ_TRANS_TYPE                   = 2;
    int    KAFKA_TRANS_TYPE                = 4;
    int    MQ_FLUME_TRANS_TYPE             = 3;

    //
    int    SUCCESS                         = 0;
    int    FAILE                           = 1;

    //文件类型
    int    FILE_TYPE_NORMAL                = 0;
    int    FILE_TYPE_ZIP                   = 1;
    int    FILE_TYPE_GZ                    = 2;
    int    FILE_TYPE_TAR_GZ                = 3;

    //收集状态
    int    FILE_COLLECT_STATUS_NORMAL      = 0;
    int    FILE_COLLECT_STATUS_PAUSE       = 1;
    int    FILE_COLLECT_STATUS_CONTINUE    = 2;
    int    FILE_COLLECT_STATUS_INTERRUPTED = 3;
    int    FILE_COLLECT_STATUS_STOP        = 4;

    //
    int    COLLECT_TYPE_PERIODICITY        = 1;
    int    COLLECT_TYPE_TEMPORALITY        = 2;
}
