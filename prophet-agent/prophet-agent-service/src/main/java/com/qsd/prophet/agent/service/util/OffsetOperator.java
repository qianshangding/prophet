package com.qsd.prophet.agent.service.util;

import com.alibaba.fastjson.JSON;
import com.kuaidadi.framework.log.ILog;
import com.kuaidadi.framework.log.LogFactory;
import com.kuaidadi.framework.thread.TaxiExecutors;
import com.kuaidadi.framework.thread.ThreadFactoryBuilder;
import com.kuaidadi.polaris.agent.biz.bean.FileInfo;
import com.kuaidadi.polaris.agent.biz.bean.FileOffSet;
import com.kuaidadi.polaris.common.exception.PolarisException;
import com.kuaidadi.polaris.common.util.JsonUtil;
import com.qsd.prophet.agent.service.HeartbeatOperator;
import com.qsd.prophet.agent.service.common.AgentConstant;
import com.qsd.prophet.agent.service.entity.FileInfo;
import com.qsd.prophet.agent.service.entity.FileOffSet;
import com.qsd.prophet.commons.exception.ProphetException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Agent日志采集元数据定时持久化类
 *
 * @author zhengyu
 */
public class OffsetOperator {
    private static final Logger logger = LoggerFactory.getLogger(OffsetOperator.class);

    /**
     * 存储各LogID对应的文件列表
     */

    private static ConcurrentHashMap<Integer, Map<String, String>> logOffsetMap = new ConcurrentHashMap<Integer, Map<String, String>>();
    private static String offsetBackPath = System.getProperty("user.home")
            + File.separator + ".logOffSet";
    /**
     * 定期保存采集的进度信息
     */
    private static ScheduledExecutorService exec = TaxiExecutors
            .newScheduledThreadPool(
                    1,
                    new ThreadFactoryBuilder(
                            "OffsetOperator-schedule-pool"));
    /**
     * 保证只初始化一次
     */
    private static volatile boolean isLoaded = false;

    static {
        /**初始化OFFSET保存路径*/
        new File(offsetBackPath).mkdirs();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    saveOffsetMap();
                } catch (Throwable e) {
                    logger.error("saveOffsetMap failed!", e);
                }
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    /**
     * 更新对应文件的OffSet
     *
     * @param fileOffSet
     */
    public static void updateLogOffset(FileOffSet fileOffSet) {
        if (logger.isDebugEnabled()) {
            logger.debug("updateLogOffset fileOffSet is " + fileOffSet);
        }
        String fileIdentity = getFileIdentityKey(fileOffSet.getLogID(), fileOffSet.getPathID(),
                fileOffSet.getLastModifyTime());
        String fileOffsetString = null;
        try {
            fileOffsetString = JSON.toJSONString(fileOffSet);
        } catch (Exception e) {
            logger.error("fileOffSet toJSONString  failed! fileOffSet is " + fileOffSet);
            return;
        }
        Map<String, String> targetLogOffSet = logOffsetMap.get(fileOffSet.getLogID());
        if (targetLogOffSet == null) {
            Map<String, String> offsetMap = new HashMap<String, String>();
            offsetMap.put(fileIdentity, fileOffsetString);
            Map<String, String> previousOffsetMap = logOffsetMap.putIfAbsent(fileOffSet.getLogID(), offsetMap);
            if (previousOffsetMap != null) {
                previousOffsetMap.put(fileIdentity, fileOffsetString);
            }
        } else {
            targetLogOffSet.put(fileIdentity, fileOffsetString);
        }
    }

    /**
     * 删除对应文件的OffSet
     *
     * @param fileInfo
     */
    public static void deleteLogOffset(FileInfo fileInfo) {
        logger.info("deleteLogOffset fileInfo  is " + fileInfo);
        Map<String, String> targetLogOffSet = logOffsetMap.get(fileInfo.getLogID());
        if (targetLogOffSet != null) {
            targetLogOffSet.remove(getFileIdentityKey(fileInfo.getLogID(), fileInfo.getPathID(),
                    fileInfo.getLastModifyTime()));
        }
        if (targetLogOffSet == null || targetLogOffSet.isEmpty()) {
            logOffsetMap.remove(fileInfo.getLogID());
        }
    }

    public static FileOffSet getTargetLogOffset(Integer logID, Integer pathID, File targetFile) throws ProphetException {
        if (logOffsetMap.size() == 0 && !isLoaded) {
            synchronized (OffsetOperator.class) {
                if (logOffsetMap.size() == 0 && !isLoaded) {
                    loadLogOffsetMap();
                    isLoaded = true;
                }
            }
        }

        Map<String, String> targeOffSetMap = logOffsetMap.get(logID);
        //在保存时断电的情况
        if (targeOffSetMap == null) {
            targeOffSetMap = logOffsetMap.get(logID + ".tmp");
        }

        if (targeOffSetMap == null) {
            return null;
        }

        String targeOffSet = targeOffSetMap.get(getFileIdentityKey(logID, pathID, targetFile == null ? null
                : targetFile.lastModified()));
        if (logger.isDebugEnabled()) {
            logger.debug("getTargetLogOffset logID is " + logID + " ,pathID is " + pathID + " ,targetFile="
                    + targetFile + " ,targeOffSet is " + targeOffSet);
        }
        return targeOffSet == null ? null : JSON.parseObject(targeOffSet, FileOffSet.class);
    }

    private static String getFileIdentityKey(Integer logID, Integer pathID, Long lastModifyTime) {
        return logID + "_" + pathID + "_" + lastModifyTime;
    }

    private static void loadLogOffsetMap() {
        Map<String, List<String>> contentMap = FileUtils.getFileContent(offsetBackPath);
        for (Entry<String, List<String>> entry : contentMap.entrySet()) {
            Map<String, String> offsetMap = new HashMap<String, String>();
            for (String content : entry.getValue()) {
                String[] splitedContents = StringUtils.split(content, AgentConstant.SEPARATORCHAR);
                if (splitedContents.length == 2) {
                    offsetMap.put(splitedContents[0], splitedContents[1]);
                }
            }
            if (offsetMap.size() > 0) {
                logOffsetMap.put(Integer.valueOf(entry.getKey()), offsetMap);
            }
        }
    }

    public static synchronized void saveOffsetMap() {

        if (logger.isDebugEnabled()) {
            logger.debug("saveOffsetMap logOffsetMap  is " + logOffsetMap);
        }

        if (logOffsetMap.isEmpty()) {
            FileUtils.emptyDirectory(offsetBackPath);

        } else {
            for (Entry<Integer, Map<String, String>> entry : logOffsetMap.entrySet()) {
                String filePath = offsetBackPath + File.separator + entry.getKey();
                List<String> contents = new ArrayList<String>();
                for (Entry<String, String> contentMapEntry : entry.getValue().entrySet()) {
                    contents.add(contentMapEntry.getKey() + AgentConstant.SEPARATORCHAR + contentMapEntry.getValue());
                }
                FileUtils.saveFileContent(filePath, contents);
            }
        }
    }

    public static void setOffsetBackPath(String offsetBackPath) {
        OffsetOperator.offsetBackPath = offsetBackPath;
        new File(offsetBackPath).mkdirs();
    }
}
