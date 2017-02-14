package com.qsd.prophet.agent.service;

import com.kuaidadi.framework.log.ILog;
import com.kuaidadi.framework.log.LogFactory;
import com.kuaidadi.polaris.agent.biz.bean.FileOffSet;
import com.kuaidadi.polaris.agent.biz.bean.LogEvent;
import com.kuaidadi.polaris.agent.biz.flume.AgentFlumeClient;
import com.kuaidadi.polaris.agent.biz.mq.AgentMqProducer;
import com.kuaidadi.polaris.agent.biz.util.AgentConstant;
import com.kuaidadi.polaris.agent.biz.util.ConfigManageBean;
import com.kuaidadi.polaris.agent.biz.util.OffsetOperator;
import com.kuaidadi.polaris.common.util.TimeUtil;
import com.qsd.prophet.agent.service.common.AgentConstant;
import com.qsd.prophet.agent.service.entity.FileOffSet;
import com.qsd.prophet.agent.service.entity.LogEvent;
import com.qsd.prophet.agent.service.util.ConfigManageBean;
import com.qsd.prophet.agent.service.util.OffsetOperator;
import com.qsd.prophet.commons.util.TimeUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 日志传输类
 *
 * @author zhangliang
 * @version $Id: FileTransfer.java, v 0.1 Jul 22, 2015 9:56:09 PM zhangliang Exp $
 */
public class FileTransfer {

    private static final Logger logger = LoggerFactory.getLogger(FileTransfer.class);
    private static final Logger perfILog = LoggerFactory.getLogger("staticslog");

    private int batchSize = 10;

    private long waitTimeInMills = 2000;

    private List<LogEvent> batchLogEvent = new ArrayList<LogEvent>();

    private long firstPutTime;

    private long maxFailedSleepTime = 32000L;

    private AgentMqProducer agentMqProducer = ServiceLocator.getAgentMqProducer();

    private AgentFlumeClient agentFlumeClient = ServiceLocator.getAgentFlumeClient();

    //private AgentKafkaProducer   agentKafkaProducer = ServiceLocator.getAgentKafkaProducer();

    private Long startTime;

    private Long endTime;

    private LogEvent latestEvent = null;

    //统计发送数量
    private Map<String, Integer> countMap = new HashMap<String, Integer>();

    /**
     * 配置类
     */
    private ConfigManageBean configManageBean = ServiceLocator.getConfigManageBean();

    public void transferLogEvent(LogEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("transferLogEvent" + " event is " + event + " , file is "
                    + (latestEvent == null ? null : latestEvent.getLogName()));
        }

        if (batchLogEvent.isEmpty()) {
            firstPutTime = System.currentTimeMillis();
        }

        if (event != null && isNeededLogEvent(event)) {
            batchLogEvent.add(event);
            latestEvent = event;

            //统计发送数量
            try {
                String timekey = TimeUtil.getStringMinute(event.getCreateTimeStamp());
                if (StringUtils.isBlank(timekey)) {
                    timekey = TimeUtil.getStringMinute(System.currentTimeMillis());
                }
                if (countMap.get(timekey) != null) {
                    countMap.put(timekey, countMap.get(timekey) + 1);
                } else {
                    if (countMap.size() > 0) {
                        for (Entry<String, Integer> entry : countMap.entrySet()) {
                            perfILog.info("logname is:" + event.getLogName() + ",collect time is:" + entry.getKey()
                                    + " count is:" + entry.getValue());
                        }
                    }
                    countMap.clear();
                    countMap.put(timekey, 1);
                }
            } catch (Exception e) {
                perfILog.info("collect occur error!", e);
            }
        }

        if (batchLogEvent.size() >= batchSize || (System.currentTimeMillis() - firstPutTime) >= waitTimeInMills) {
            sendEvent();
        } else {
            if (event != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("event is hold for batch send" + " event is " + event);
                }
            }
        }
    }

    public void flush() {
        sendEvent();
        OffsetOperator.saveOffsetMap();
    }

    private void sendEvent() {

        if (batchLogEvent.isEmpty()) {
            return;
        }

        // 失败则采用退避算法不断的阻塞重试
        if (batchTransferLogEvent() == AgentConstant.FAILE) {
            long initalSleepTime = 1000L;
            while (true) {
                try {
                    Thread.sleep(initalSleepTime);
                } catch (Exception e) {
                    logger.error("batchTransferLogEvent sleep was interrupted!", e);
                }
                if (batchTransferLogEvent() == AgentConstant.SUCCESS) {
                    break;
                }
                initalSleepTime *= 2;
                if (initalSleepTime > maxFailedSleepTime) {
                    initalSleepTime = 1000L;
                }
            }
        }

        // 记录OffSet
        FileOffSet fileOffSet = new FileOffSet();
        fileOffSet.setFileName(latestEvent.getLogName());
        fileOffSet.setLogID(latestEvent.getLogID());
        fileOffSet.setPathID(latestEvent.getPathID());
        fileOffSet.setOffSet(latestEvent.getOffSet());
        fileOffSet.setParentPath(latestEvent.getParentPath());
        fileOffSet.setTimeStamp(latestEvent.getCreateTimeStamp());
        fileOffSet.setLastModifyTime(latestEvent.getLastModifyTime());
        OffsetOperator.updateLogOffset(fileOffSet);

        batchLogEvent.clear();
    }

    private int batchTransferLogEvent() {
        logger.info("batchTransferLogEvent batchLogEvent size " + batchLogEvent.size() + " file is "
                + (latestEvent == null ? null : latestEvent.getLogName()));
        int transType = batchLogEvent.get(0).getTransType();
        int useType = batchLogEvent.get(0).getUseType();

        // JStorm同时支持MQ与FLUME的消费
        if (useType == AgentConstant.LOG_FOR_JSTORM) {
            if (transType == AgentConstant.FLUME_TRANS_TYPE) {
                if (batchTransferLogEventByFLUME() == AgentConstant.FAILE) {
                    return batchTransferLogEventByMQ();
                }
                return AgentConstant.SUCCESS;
            } else if (transType == AgentConstant.MQ_TRANS_TYPE) {
                if (batchTransferLogEventByMQ() == AgentConstant.FAILE) {
                    return batchTransferLogEventByFLUME();
                }
                return AgentConstant.SUCCESS;
            } else {
                return AgentConstant.FAILE;
            }
        } else {
            if (transType == AgentConstant.FLUME_TRANS_TYPE) {
                return batchTransferLogEventByFLUME();
            } else if (transType == AgentConstant.MQ_TRANS_TYPE) {
                return batchTransferLogEventByMQ();
            } else {
                if (batchTransferLogEventByFLUME() == AgentConstant.FAILE) {
                    return batchTransferLogEventByMQ();
                }
                return AgentConstant.SUCCESS;
            }
        }
    }

    private int batchTransferLogEventByMQ() {
        if (logger.isDebugEnabled()) {
            logger.debug("batchTransferLogEventByMQ batchLogEvent are " + batchLogEvent);
        }
        if (agentMqProducer.sendLogEvent(batchLogEvent)) {
            return AgentConstant.SUCCESS;
        }
        return AgentConstant.FAILE;
    }

    //private int batchTransferLogEventByKafka() {
    //    if (logger.isDebugEnabled()) {
    //        logger.debug("batchTransferLogEventByKafka batchLogEvent are " + batchLogEvent);
    //    }
    //    if (agentKafkaProducer.sendLogEvent(batchLogEvent)) {
    //        return AgentConstant.SUCCESS;
    //    }
    //    return AgentConstant.FAILE;
    //}

    private int batchTransferLogEventByFLUME() {
        if (logger.isDebugEnabled()) {
            logger.debug("batchTransferLogEventByFLUME batchLogEvent are " + batchLogEvent);
        }
        if (agentFlumeClient.sendLogEvent(batchLogEvent)) {
            return AgentConstant.SUCCESS;
        }
        return AgentConstant.FAILE;
    }

    private boolean isNeededLogEvent(LogEvent logEvent) {

        boolean result = true;

        long createTimeStamp = logEvent.getCreateTimeStamp();
        if (startTime != null) {
            result &= createTimeStamp >= startTime;
        }
        if (endTime != null) {
            result &= createTimeStamp <= endTime;
        }

        return result;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setWaitTimeInMills(long waitTimeInMills) {
        this.waitTimeInMills = waitTimeInMills;
    }

    public void setBatchLogEvent(List<LogEvent> batchLogEvent) {
        this.batchLogEvent = batchLogEvent;
    }

    public void setMaxFailedSleepTime(long maxFailedSleepTime) {
        this.maxFailedSleepTime = maxFailedSleepTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public FileTransfer() {
        if (configManageBean != null) {
            try {
                batchSize = configManageBean.getMqbatchSize();
            } catch (Exception e) {
                logger.error("configManageBean getMqbatchSize failed", e);
            }
        }
    }
}
