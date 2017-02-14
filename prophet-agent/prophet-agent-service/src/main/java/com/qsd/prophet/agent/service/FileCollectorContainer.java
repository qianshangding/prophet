package com.qsd.prophet.agent.service;

import com.kuaidadi.framework.log.ILog;
import com.kuaidadi.framework.log.LogFactory;
import com.kuaidadi.framework.thread.TaxiExecutors;
import com.kuaidadi.framework.thread.TaxiThreadPoolExecutor;
import com.kuaidadi.framework.thread.ThreadFactoryBuilder;
import com.kuaidadi.polaris.agent.biz.bean.FileInfo;
import com.kuaidadi.polaris.agent.biz.util.AgentConstant;
import com.kuaidadi.polaris.agent.biz.util.CommonUtil;
import com.kuaidadi.polaris.agent.biz.util.FileUtil;
import com.kuaidadi.polaris.config.beans.common.LogBasicInfo;
import com.qsd.prophet.agent.service.entity.FileInfo;
import com.qsd.prophet.config.entity.LogBasicInfo;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * 日志采集容器类，用于统计采集相关的信息，定时接受从MQ推送过来的更新消息，主要用于与Config模块在运行时交互
 * @author zhangliang
 * @version $Id: FileCollectorMgr.java, v 0.1 Jul 22, 2015 9:56:20 PM zhangliang Exp $
 */
public class FileCollectorContainer {

    private static final Logger logger = LoggerFactory.getLogger(FileCollectorContainer.class);

    private static ConcurrentHashMap<FileInfo, FileCollector>     existCollectorMap            = new ConcurrentHashMap<FileInfo, FileCollector>();

    private static ConcurrentHashMap<String, List<FileCollector>> logIDPathID2FileCollectorMap = new ConcurrentHashMap<String, List<FileCollector>>();

    private static ExecutorService                                executorService              = new TaxiThreadPoolExecutor(
                                                                                                   24,
                                                                                                   256,
                                                                                                   0L,
                                                                                                   TimeUnit.MILLISECONDS,
                                                                                                   new LinkedBlockingQueue<Runnable>(),
                                                                                                   new ThreadFactoryBuilder(
                                                                                                       "FileCollectorContainer-fix-pool"));

    private static ScheduledExecutorService                       scheduledService             = TaxiExecutors
                                                                                                   .newScheduledThreadPool(
                                                                                                       2,
                                                                                                       new ThreadFactoryBuilder(
                                                                                                           "fileNotExistCollector-pool"));

    private static ConcurrentHashMap<String, FileInfo>            fileNotExistCollectorMap     = new ConcurrentHashMap<String, FileInfo>();

    public synchronized static void addToCollectorContainer(FileInfo fileInfo) {
        logger.info("addToCollectorContainer fileInfo is " + fileInfo);

        if (FileUtil.fileIsEmpty(fileInfo.getAbsFilePath())) {
            logger.info("put fileInfo to fileNotExistCollectorMap,fileInfo is:" + fileInfo);
            fileNotExistCollectorMap.put(fileInfo.getLogIDAndPathIDKey(), fileInfo);
            return;
        }

        FileCollector fileCollector = existCollectorMap.get(fileInfo);
        if (fileCollector == null) {
            fileCollector = new FileCollector(fileInfo);
            existCollectorMap.put(fileInfo, fileCollector);
            executorService.submit(fileCollector);

        } else {
            logger.warn("addToCollectorContainer failed! fileInfo already exist!, fileInfo is " + fileInfo);
        }

        List<FileCollector> logIDPathID2FileCollectors = logIDPathID2FileCollectorMap.get(fileInfo
            .getLogIDAndPathIDKey());
        if (logIDPathID2FileCollectors == null) {
            logIDPathID2FileCollectors = new ArrayList<FileCollector>();
            logIDPathID2FileCollectorMap.put(fileInfo.getLogIDAndPathIDKey(), logIDPathID2FileCollectors);
        }
        logIDPathID2FileCollectors.add(fileCollector);
    }

    public synchronized static void stopCollectLog() {
        logger.info("stopCollectLog!");
        for (FileCollector fileCollector : existCollectorMap.values()) {
            fileCollector.setFileCollectStatus(AgentConstant.FILE_COLLECT_STATUS_STOP);
        }
    }

    public synchronized static void delFromCollectorContainer(FileInfo fileInfo) {
        logger.info("delFromCollectorContainer fileInfo is " + fileInfo);

        FileCollector fileCollector = existCollectorMap.get(fileInfo);
        if (fileCollector != null) {
            fileCollector.setFileCollectStatus(AgentConstant.FILE_COLLECT_STATUS_INTERRUPTED);
            existCollectorMap.remove(fileInfo);
        } else {
            logger.warn("delFromCollectorContainer failed! fileInfo already not exist!, fileInfo is " + fileInfo);
        }

        List<FileCollector> logIDPathID2FileCollectors = logIDPathID2FileCollectorMap.get(fileInfo
            .getLogIDAndPathIDKey());
        if (logIDPathID2FileCollectors != null) {
            for (FileCollector element : logIDPathID2FileCollectors) {
                element.setFileCollectStatus(AgentConstant.FILE_COLLECT_STATUS_INTERRUPTED);
            }
            logIDPathID2FileCollectorMap.remove(fileInfo.getLogIDAndPathIDKey());
        }
        //删除还没有采集进程的采集信息
        synchronized (fileNotExistCollectorMap) {
            fileNotExistCollectorMap.remove(fileInfo.getLogIDAndPathIDKey());
        }

    }

    public synchronized static void updateToCollectorContainer(FileInfo fileInfo) {
        logger.info("updateToCollectorContainer fileInfo is " + fileInfo);

        List<FileCollector> logIDPathID2FileCollectors = logIDPathID2FileCollectorMap.get(fileInfo
            .getLogIDAndPathIDKey());
        if (CollectionUtils.isNotEmpty(logIDPathID2FileCollectors)) {
            for (FileCollector fileCollector : logIDPathID2FileCollectors) {
                fileCollector.setFileCollectStatus(AgentConstant.FILE_COLLECT_STATUS_INTERRUPTED);
                fileCollector.getFileInfo().reset(fileInfo);
                fileCollector.setFileCollectStatus(AgentConstant.FILE_COLLECT_STATUS_CONTINUE);
            }
        } else {
            logger.warn("updateToCollectorContainer failed! fileInfo not exist!, fileInfo is " + fileInfo);
        }
        //更新还没有采集进程的采集信息
        synchronized (fileNotExistCollectorMap) {
            fileNotExistCollectorMap.remove(fileInfo.getLogIDAndPathIDKey());
            fileNotExistCollectorMap.put(fileInfo.getLogIDAndPathIDKey(), fileInfo);
        }
    }

    public static void launchFileCollector(List<LogBasicInfo> logBasicInfoLst) {
        List<FileInfo> fileInfoLst = CommonUtil.getFileInfoFromLogBasicInfo(logBasicInfoLst);
        if (fileInfoLst == null || fileInfoLst.size() == 0) {
            return;
        }
        for (FileInfo fileInfo : fileInfoLst) {
            addToCollectorContainer(fileInfo);
        }
        //启动扫描没有日志的日志文件
        scheduledService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                synchronized (fileNotExistCollectorMap) {
                    for (Entry<String, FileInfo> entry : fileNotExistCollectorMap.entrySet()) {
                        if (entry.getValue() != null && !FileUtil.fileIsEmpty(entry.getValue().getAbsFilePath())) {
                            addToCollectorContainer(entry.getValue());
                            fileNotExistCollectorMap.remove(entry.getKey());
                            logger.info("remvoe fileInfo from fileNotExistCollectorMap,fileInfo is:" + entry.getKey());
                        }
                    }
                }
            }
        }, 2, 30, TimeUnit.SECONDS);
    }

    public static ConcurrentHashMap<String, List<FileCollector>> getLogIDPathID2FileCollectorMap() {
        return logIDPathID2FileCollectorMap;
    }
}
