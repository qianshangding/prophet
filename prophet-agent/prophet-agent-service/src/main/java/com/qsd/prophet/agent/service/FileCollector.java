package com.qsd.prophet.agent.service;

import com.qsd.prophet.agent.service.common.AgentConstant;
import com.qsd.prophet.agent.service.entity.FileInfo;
import com.qsd.prophet.agent.service.entity.FileOffSet;
import com.qsd.prophet.agent.service.entity.LogEvent;
import com.qsd.prophet.agent.service.util.FileUtils;
import com.qsd.prophet.agent.service.util.OffsetOperator;
import com.qsd.prophet.commons.constant.Constants;
import com.qsd.prophet.commons.util.HostNameUtil;
import com.qsd.prophet.commons.util.TimeUtil;
import com.qsd.prophet.config.api.Config;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;

/**
 * 具体日志的采集类所有变量都是volatile ，主要是解决内部监控线程的可见性问题
 *
 * @author zhengyu
 */
public class FileCollector implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FileCollector.class);

    private volatile RandomAccessFile in = null;

    /**
     * 文件元信息
     */
    private volatile FileInfo fileInfo = null;

    /**
     * 当前文件是否采集完成
     */
    private volatile boolean isCollectFinsh = false;

    /**
     * 当前采集状态
     */
    private volatile int fileCollectStatus = AgentConstant.FILE_COLLECT_STATUS_NORMAL;

    /**
     * 当前采集的offSet
     */
    private volatile long currentOffSet = 0;

    /**
     * 当前采集的时间戳
     */
    private volatile Long timeStamp;

    /**
     * 当前采集的可读格式时间
     */
    private volatile String timeString;

    /**
     * 开始采集时间
     */
    private volatile Long startTimeStamp;

    /**
     * 结束采集时间
     */
    private volatile Long endTimeStamp;

    /**
     * 日志传输类
     */
    private volatile FileTransfer fileTransfer;

    /**
     * 没有日志产生，又未结束时，内部先重读几次，如果依旧未读到数据，则Sleep一段时间
     */
    private volatile Integer maxIdleRetryTime = 10;

    /**
     * 空闲休息 1S
     */
    private volatile Long idleSleepTime = 3000L;
    /**
     * 如果该路径还没有日志文件等待时间
     */
    private final Long emptyFileWaitTime = 30000L;

    /**
     * 文件类型
     */
    private volatile Integer fileType = AgentConstant.FILE_TYPE_NORMAL;

    /**
     * 实际采集的文件
     */
    private volatile String actualFilePath;

    /**
     * 采集为文件滚动监控类
     */
    private volatile boolean isStopped = false;

    /**
     * 目录监控相关的问题
     */
    private volatile WatchService watchService;

    /**
     * 目录监控相关的线程
     */
    private volatile Thread watchThread;

    /**
     * 日志上传心跳类
     */
    private HeartbeatOperator heartbeatOperator = ServiceLocator.getHeartbeatOperator();

    public FileCollector(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.currentOffSet = fileInfo.getOffSet() == null ? 0 : fileInfo.getOffSet();
        this.startTimeStamp = fileInfo.getStartTime();
        this.endTimeStamp = fileInfo.getEndTime();
        this.actualFilePath = fileInfo.getAbsFilePath();
        this.fileType = FileUtils.getFileType(fileInfo.getAbsFilePath());
        this.fileTransfer = new FileTransfer();
        fileTransfer.setStartTime(startTimeStamp);
        fileTransfer.setEndTime(endTimeStamp);
        monitorFileRollOver();
    }

    private void open() throws IOException {
        synchronized (this) {
            in = new RandomAccessFile(actualFilePath, "r");
            in.seek(currentOffSet);
        }
    }

    private void close() {
        synchronized (this) {
            try {
                if (in != null && !isStopped) {
                    in.close();
                    isStopped = true;
                }
            } catch (Exception e) {
                logger.error("file close failed!, fileInfo is " + fileInfo, e);
            }
        }
    }

    private String readLineSeperateByTimeFromFile() throws IOException {
        synchronized (this) {
            StringBuffer sb = new StringBuffer();
            boolean haveFirstLine = false;
            //防止文件突然被清空
            if (currentOffSet > in.length()) {
                currentOffSet = in.length();
            }
            in.seek(currentOffSet);
            String line = in.readLine();
            String suspectTimeString = null;
            String encodeString = null;
            /** 以一行为开始，另一行的开始为结束，标示一个逻辑上完整的日志行，行开始的标示是是否包含时间戳*/
            while (line != null) {
                encodeString = new String(line.getBytes("ISO-8859-1"), fileInfo.getEncodeType());
                suspectTimeString = FileUtils.getTimeStringFormLine(encodeString, fileInfo);
                if (TimeUtil.isTimeFormat(suspectTimeString, fileInfo.getTimeFormat())) {
                    if (haveFirstLine) {
                        break;
                    } else {
                        timeStamp = TimeUtil.getLongTimeStamp(suspectTimeString, fileInfo.getTimeFormat());
                        timeString = suspectTimeString;
                        sb.append(encodeString);
                        haveFirstLine = true;
                        currentOffSet = in.getFilePointer();
                    }
                } else {
                    if (haveFirstLine) {
                        sb.append("\n");
                        sb.append(encodeString);
                        currentOffSet = in.getFilePointer();
                    }
                }
                line = in.readLine();
            }
            return sb.toString();
        }
    }

    /**
     * 检测当前的文件是否出现了滚动，新生成的文件也进行并行的采集
     */
    private void monitorFileRollOver() {
        if (fileInfo.getIsLatestFile()) {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                Paths.get(new File(fileInfo.getAbsFilePath()).getParent()).register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE);
            } catch (Exception e) {
                logger.error("watchService init failed! file is " + fileInfo.getLogName());
                return;
            }
            watchThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!isStopped) {
                        WatchKey key;
                        try {
                            key = watchService.take();
                        } catch (InterruptedException ex) {
                            logger.info("watchService.take() failed!", ex);
                            continue;
                        }
                        if (key == null) {
                            continue;
                        }
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            Path target = (Path) event.context();
                            if (logger.isDebugEnabled()) {
                                logger.debug("file is created  Path is " + target);
                            }
                            if (fileInfo.getLogName().equalsIgnoreCase(target.toString())) {
                                logger.info("monitorFileRollOver triggered ,target file is " + target);
                                /**获取最新的文件信息，这个必须放在首行*/
                                FileInfo masterFileInfo = fileInfo.getRefreshFileInfo();

                                /**获取当前应该采集文件信息*/
                                File rolledFile = FileUtils.getLatestRolledFile(fileInfo.getParentPath(),
                                        fileInfo.getLogName());
                                logger.info("rolledFile file is " + rolledFile + ", master file is "
                                        + masterFileInfo.getLogName());
                                if (rolledFile == null) {
                                    logger.warn("monitorFileRollOver run abnormal on onFileCreate!");
                                    return;
                                }

                                fileCollectStatus = AgentConstant.FILE_COLLECT_STATUS_PAUSE;
                                fileInfo.setIsLatestFile(false);
                                fileInfo.setLogName(rolledFile.getName());
                                fileInfo.setCollectType(AgentConstant.COLLECT_TYPE_TEMPORALITY);
                                fileInfo.setLastModifyTime(rolledFile.lastModified());
                                actualFilePath = fileInfo.getParentPath() + File.separator + rolledFile.getName();

                                /**关闭当前文件句柄，打开新的文件句柄，如果打开新文件句柄失败，则中断采集,并对文件的采集进度进行对应的调整*/
                                try {
                                    close();

                                    FileOffSet rolledFileOffSet = new FileOffSet();
                                    rolledFileOffSet.setFileName(rolledFile.getName());
                                    rolledFileOffSet.setLogID(fileInfo.getLogID());
                                    rolledFileOffSet.setOffSet(currentOffSet);
                                    rolledFileOffSet.setParentPath(fileInfo.getParentPath());
                                    rolledFileOffSet.setPathID(fileInfo.getPathID());
                                    rolledFileOffSet.setTimeStamp(timeStamp);
                                    rolledFileOffSet.setLastModifyTime(rolledFile.lastModified());
                                    OffsetOperator.updateLogOffset(rolledFileOffSet);
                                    OffsetOperator.saveOffsetMap();

                                    open();
                                } catch (Throwable e) {
                                    logger.error("open file failed in monitorFileRollOver ", e);
                                    fileCollectStatus = AgentConstant.FILE_COLLECT_STATUS_INTERRUPTED;
                                }
                                /** 提交新采集任务*/
                                FileCollectorContainer.addToCollectorContainer(masterFileInfo);
                                fileCollectStatus = AgentConstant.FILE_COLLECT_STATUS_NORMAL;
                            }
                        }
                        if (!key.reset()) {
                            logger.warn("stop moniotr file channage for unknown reason!");
                            break;
                        }
                    }
                }
            }, fileInfo.getLogName() + "_" + TimeUtil.getCurrentTime(TimeUtil.YYYY_MM_DD_HH_MM_SS));
            watchThread.start();
        }
    }

    private LogEvent getLogEventFromFile() throws IOException {
        LogEvent logEvent = null;
        String content = readLineSeperateByTimeFromFile();
        if (StringUtils.isNotEmpty(content)) {
            logEvent = fileInfo.getCommonLogEvent();
            logEvent.setContent(content);
            logEvent.setHostName(HostNameUtil.HOSTNAME);
            logEvent.setCreateTimeStamp(timeStamp);
            logEvent.setCreateTimeString(timeString);
            logEvent.setParentPath(fileInfo.getParentPath());
            logEvent.setOffSet(currentOffSet);
            logEvent.setLastModifyTime(fileInfo.getLastModifyTime());
        }
        return logEvent;
    }

    public void run() {

        try {
            logger.info("begin to collect file [ " + fileInfo.getAbsFilePath() + " ]");
            File actualFile = new File(actualFilePath);

            /**如果是压缩文件，则先进行解压缩*/
            if (fileType != AgentConstant.FILE_TYPE_NORMAL) {
                if (fileInfo.getCollectType() != AgentConstant.COLLECT_TYPE_TEMPORALITY) {
                    logger.error("Illegal argument file is compressed while collet type is not tempory! file is "
                            + fileInfo.getAbsFilePath());
                    return;
                }

                actualFilePath = actualFilePath.substring(0, actualFilePath.lastIndexOf('.'));
                if (fileType == AgentConstant.FILE_TYPE_TAR_GZ) {
                    actualFilePath = actualFilePath.substring(0, actualFilePath.lastIndexOf('.'));
                }
                actualFile = new File(actualFilePath);
                if (!decompressFile(fileInfo.getAbsFilePath(), actualFilePath)) {
                    logger.error("decompressFile  failed! file is " + fileInfo.getAbsFilePath());
                    return;
                }
            }

            try {
                open();
            } catch (Throwable ex) {
                logger.error("fileCollector open  failed! file is " + fileInfo.getAbsFilePath(), ex);
                return;
            }

            LogEvent logEvent = null;
            int idleRetryTime = 0;
            Config configSevice = ServiceLocator.getConfigService();

            for (; ; ) {
                if (fileCollectStatus == AgentConstant.FILE_COLLECT_STATUS_INTERRUPTED) {
                    logger.info("fileCollector was " + "breaked file is " + fileInfo.getAbsFilePath());
                    isCollectFinsh = true;
                    break;
                }

                if (fileCollectStatus == AgentConstant.FILE_COLLECT_STATUS_STOP) {
                    fileTransfer.flush();
                    logger.info("fileCollector was " + "stop, file is " + fileInfo.getAbsFilePath());
                    isCollectFinsh = true;
                    break;
                }

                if (fileCollectStatus == AgentConstant.FILE_COLLECT_STATUS_PAUSE) {
                    try {
                        Thread.sleep(idleSleepTime);
                    } catch (InterruptedException e) {
                        logger.error("idleSleep is Interrupted!", e);
                    }
                    continue;
                }

                if (isCollectFinsh && fileInfo.getCollectType() == AgentConstant.COLLECT_TYPE_TEMPORALITY) {
                    fileTransfer.flush();
                    logger.info("fileCollector finished ! file is " + fileInfo.getAbsFilePath());
                    break;
                }

                try {
                    logEvent = getLogEventFromFile();
                } catch (IOException e) {
                    fileTransfer.flush();
                    logger.error("getLogEventFromFile failed! file is " + fileInfo.getAbsFilePath(), e);
                    isCollectFinsh = true;
                    break;
                }

                /**不管有没有日志都调用，用于将内存中累计的过期日志及时推送过去,心跳信息也要及时的更新*/
                fileTransfer.transferLogEvent(logEvent);
                /**当前采集的主文件需要上报心跳消息*/
                if (fileInfo.getIsLatestFile()) {
                    heartbeatOperator.statHeartBeatInfo(fileInfo,
                            logEvent == null ? null : logEvent.getCreateTimeString());
                }
                if (logEvent != null) {
                    idleRetryTime = 0;
                } else {
                    if (fileInfo.getCollectType() == AgentConstant.COLLECT_TYPE_TEMPORALITY) {
                        if (in.read() == -1) {
                            isCollectFinsh = true;
                        }
                    } else {
                        idleRetryTime++;
                        if (idleRetryTime > maxIdleRetryTime) {
                            logger
                                    .info("getLogEventFromFile reach maxIdleRetryTime ,we will sleep for a while! file is "
                                            + fileInfo.getLogName());
                            try {
                                Thread.sleep(idleSleepTime);
                            } catch (InterruptedException e) {
                                logger.error("idleSleep is Interrupted file is " + fileInfo.getAbsFilePath(), e);
                            }
                        }
                    }
                }
            }

            close();

            if (isCollectFinsh) {
                if (fileInfo.getIsLatestFile() && fileInfo.getCollectType() == AgentConstant.COLLECT_TYPE_TEMPORALITY) {
                    /**更新配置中心，告知临时采集任务采集结束*/
                    try {
                        if (configSevice != null) {
                            configSevice.updateLogCollectStatus(HostNameUtil.HOSTNAME, fileInfo.getPathID(),
                                    Constants.COLLECT_STATUS_FINISH);
                        }
                    } catch (Exception e) {
                        logger.error("updateLogCollectStatus failed!, file is " + fileInfo.getAbsFilePath(), e);
                    }
                }

                /**停止监控服务*/
                if (watchThread != null) {
                    watchThread.interrupt();
                }

                if (watchService != null) {
                    watchService.close();
                }

                /**删除记录采集的进度*/
                if (fileCollectStatus != AgentConstant.FILE_COLLECT_STATUS_STOP) {
                    OffsetOperator.deleteLogOffset(fileInfo);
                }
            }
            /** 采集完成之后，删除解压后的文件*/
            if (fileType != AgentConstant.FILE_TYPE_NORMAL) {
                actualFile.delete();
            }
        } catch (Throwable e) {
            logger.error("unexpected error, fileCollector is " + this, e);
        }
    }

    private boolean decompressFile(String absFilePath, String actualFilePath) {
        if (fileType == AgentConstant.FILE_TYPE_GZ) {
            return FileUtils.extractGzipFile(absFilePath, actualFilePath);
        }
        if (fileType == AgentConstant.FILE_TYPE_TAR_GZ) {
            return FileUtils.extractTarGzFile(absFilePath, actualFilePath);
        }
        if (fileType == AgentConstant.FILE_TYPE_ZIP) {
            return FileUtils.extractZipFile(absFilePath, actualFilePath);
        }
        return false;
    }

    public void setFileCollectStatus(int fileCollectStatus) {
        this.fileCollectStatus = fileCollectStatus;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public Long getCurrentOffSet() {
        return currentOffSet;
    }

    public void setCurrentOffSet(Long currentOffSet) {
        this.currentOffSet = currentOffSet;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public void setMaxIdleRetryTime(int maxIdleRetryTime) {
        this.maxIdleRetryTime = maxIdleRetryTime;
    }

    public String getFileCollectorKey() {
        return fileInfo.getLogID() + "_" + fileInfo.getPathID() + "_" + fileInfo.getLogName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileInfo == null) ? 0 : fileInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileCollector other = (FileCollector) obj;
        if (fileInfo == null) {
            if (other.fileInfo != null)
                return false;
        } else if (!fileInfo.equals(other.fileInfo))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FileCollector [fileInfo=" + fileInfo + ", isCollectFinsh=" + isCollectFinsh + ", fileCollectStatus="
                + fileCollectStatus + ", currentOffSet=" + currentOffSet + ", timeStamp=" + timeStamp + ", timeString="
                + timeString + ", startTimeStamp=" + startTimeStamp + ", endTimeStamp=" + endTimeStamp
                + ", maxIdleRetryTime=" + maxIdleRetryTime + ", idleSleepTime=" + idleSleepTime + ", fileType="
                + fileType + ", actualFilePath=" + actualFilePath + "]";
    }
}
