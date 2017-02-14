package com.qsd.prophet.agent.node;

import com.qsd.prophet.agent.service.FileCollectorContainer;
import com.qsd.prophet.commons.util.HostNameUtil;
import com.qsd.prophet.config.api.Config;
import com.qsd.prophet.config.entity.LogBasicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

/**
 * Agent启动类
 *
 * @author zhengyu
 */
public class AgentLauncher {
    private static final Logger logger = LoggerFactory.getLogger(AgentLauncher.class);

    public static void main(String[] args) {
        startAgent();
    }

    public static void startAgent() {
        /**初始化xml变动监控*/
        LogConfigListener.init();

        List<LogBasicInfo> logBasicInfoLst = null;
        /**从ConfigServer拉取采集的日志信息*/
        try {
            @SuppressWarnings("resource")
            Config configService = (Config) new ClassPathXmlApplicationContext("spring/polaris-agent-biz.xml")
                    .getBean("configService");
            logBasicInfoLst = configService.getLogInfoByHost(HostNameUtil.HOSTNAME);
            logger.info("getLogInfoByHost logBasicInfoLst is " + logBasicInfoLst);
            AgentConfigPersistence.persistAgentConfig(logBasicInfoLst);
        } catch (Throwable e) {
            logger.error("getLogInfoByApps from dubbo failed!", e);
            logBasicInfoLst = AgentConfigPersistence.loadAgentConfigFromFile();
        }

        try {
            /**根据LogBasicInfo构造、分发、启动日志采集任务*/
            FileCollectorContainer.launchFileCollector(logBasicInfoLst);
        } catch (Exception e) {
            logger.error("launchFileCollector  failed!", e);
        }
        //统一offerst和消息发送
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                FileCollectorContainer.stopCollectLog();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("sleep when stopCollectLog failed!", e);
                }
                logger.info("stopCollectLog sleep done!");
            }
        });

        System.out.println(" Agent launcher start ok!!!");
    }
}
