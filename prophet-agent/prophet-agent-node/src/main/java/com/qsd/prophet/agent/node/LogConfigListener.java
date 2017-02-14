package com.qsd.prophet.agent.node;

import org.springframework.util.Log4jConfigurer;

import java.io.FileNotFoundException;

/**
 * 定时扫描加载最新的Log4j配置文件. 通过Spring ioc 管理该Bean, 并执行初始化init()方法即可
 * 
 * @author zhangliang
 * 
 */
public class LogConfigListener {

    private static int    refreshInterval = 60;                   // 刷新间隔, 默认60秒

    private static String fileLocation    = "classpath:log4j.xml";

    public static void init() {
        try {
            Log4jConfigurer.initLogging(fileLocation, refreshInterval);
        } catch (FileNotFoundException e) {
            System.err.print("Log4j config not found. " + fileLocation);
        }
    }

    public static void destroy() {
        Log4jConfigurer.shutdownLogging();
    }
}
