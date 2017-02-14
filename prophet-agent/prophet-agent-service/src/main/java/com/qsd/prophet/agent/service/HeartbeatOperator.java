package com.qsd.prophet.agent.service;

import com.kuaidadi.framework.log.ILog;
import com.kuaidadi.framework.log.LogFactory;
import com.kuaidadi.framework.thread.TaxiExecutors;
import com.kuaidadi.framework.thread.ThreadFactoryBuilder;
import com.kuaidadi.polaris.agent.biz.bean.FileInfo;
import com.kuaidadi.polaris.agent.biz.mq.AgentMqProducer;
import com.kuaidadi.polaris.agent.biz.util.CommonUtil;
import com.kuaidadi.polaris.beans.message.AgentHeartBeatInfo;
import com.kuaidadi.polaris.beans.message.FileCollcetHeartbeatInfo;
import com.kuaidadi.polaris.common.util.TimeUtil;
import com.qsd.prophet.agent.service.entity.AgentHeartBeatInfo;
import com.qsd.prophet.agent.service.entity.FileCollcetHeartbeatInfo;
import com.qsd.prophet.agent.service.entity.FileInfo;
import com.qsd.prophet.commons.util.HostNameUtil;
import com.qsd.prophet.commons.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志采集心跳保持类，主要发送给Config模块，用于Agent的监控
 *
 * @author zhengyu
 */
@Service
public class HeartbeatOperator {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatOperator.class);

    private ConcurrentHashMap<FileInfo, FileCollcetHeartbeatInfo> currentHeartBeatMap = new ConcurrentHashMap<FileInfo, FileCollcetHeartbeatInfo>();
    private ConcurrentHashMap<String, AtomicInteger> minuteStatMap = new ConcurrentHashMap<String, AtomicInteger>();

    @Autowired
    private AgentMqProducer agentMqProducer;

    public void statHeartBeatInfo(FileInfo fileInfo, String timeStamp) {
        FileCollcetHeartbeatInfo fileCollcetHeartbeatInfo = currentHeartBeatMap.get(fileInfo);
        if (fileCollcetHeartbeatInfo == null) {
            fileCollcetHeartbeatInfo = new FileCollcetHeartbeatInfo();
            fileCollcetHeartbeatInfo.setLogID(fileInfo.getLogID());
            fileCollcetHeartbeatInfo.setCurrentCollectCount(0);
            fileCollcetHeartbeatInfo.setLogName(fileInfo.getLogName());
            fileCollcetHeartbeatInfo.setPathID(fileInfo.getPathID());
            currentHeartBeatMap.putIfAbsent(fileInfo, fileCollcetHeartbeatInfo);
        }
        if (timeStamp != null) {
            String logStatKey = fileInfo.getLogIDAndPathIDKey() + "_" + fileInfo.getLogName() + "_"
                    + TimeUtil.getLongTimeStamp(timeStamp, fileInfo.getTimeFormat()).toString();
            fileCollcetHeartbeatInfo.incrCurrentCollectCount();
            fileCollcetHeartbeatInfo.setCurrentCollectTimestamp(timeStamp);
            AtomicInteger statNum = minuteStatMap.get(logStatKey);
            if (statNum == null) {
                minuteStatMap.putIfAbsent(logStatKey, new AtomicInteger(1));
            } else {
                statNum.incrementAndGet();
            }
        }

        fileCollcetHeartbeatInfo.setHeartBeatTime(System.currentTimeMillis());
    }

    /**
     * 定期上报Agent心跳信息
     */
    private static ScheduledExecutorService exec = TaxiExecutors.newScheduledThreadPool(1, new ThreadFactoryBuilder(
            "HeartbeatOperator-schedule-pool"));

    {
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    uploadHeartBeatInfo();
                    outputPerfLog();
                } catch (Throwable e) {
                    logger.error("uploadHeartBeatInfo failed!", e);
                }
            }
        }, 5, 60, TimeUnit.SECONDS);
    }

    public void uploadHeartBeatInfo() {

        AgentHeartBeatInfo agentHeartBeatInfo = new AgentHeartBeatInfo();
        agentHeartBeatInfo.setUploadHeartBeatTime(TimeUtil.getCurrentTime(TimeUtil.YYYY_MM_DD_HH_MM_SS));
        agentHeartBeatInfo.setHostName(HostNameUtil.HOSTNAME);

        ConcurrentHashMap<FileInfo, FileCollcetHeartbeatInfo> uploadHeartBeatMap = currentHeartBeatMap;
        currentHeartBeatMap = new ConcurrentHashMap<FileInfo, FileCollcetHeartbeatInfo>();
        List<FileCollcetHeartbeatInfo> fileCollcetHeartbeatInfos = new ArrayList<FileCollcetHeartbeatInfo>();
        for (Entry<FileInfo, FileCollcetHeartbeatInfo> entry : uploadHeartBeatMap.entrySet()) {
            fileCollcetHeartbeatInfos.add(entry.getValue());
        }

        agentHeartBeatInfo.setFileCollcetHeartbeatInfos(fileCollcetHeartbeatInfos);

        logger.info("uploadHeartBeatInfo agentHeartBeatInfo is " + agentHeartBeatInfo);
        agentMqProducer.sendAgentHeartBeatInfo(agentHeartBeatInfo);
    }

    public void outputPerfLog() {

        ConcurrentHashMap<String, AtomicInteger> currentStatMap = minuteStatMap;
        minuteStatMap = new ConcurrentHashMap<String, AtomicInteger>();
        Iterator<Entry<String, AtomicInteger>> it = currentStatMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, AtomicInteger> entry = it.next();
            logger.info(entry.getKey() + " : " + entry.getValue());
            it.remove();
        }
    }
}
