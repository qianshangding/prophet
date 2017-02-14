package com.qsd.prophet.agent.node;

import com.alibaba.fastjson.JSON;
import com.qsd.prophet.agent.service.util.FileUtils;
import com.qsd.prophet.config.entity.LogBasicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AgentConfigPersistence {

    private static final Logger logger = LoggerFactory.getLogger(AgentConfigPersistence.class);
    private static String configBackPath = System.getProperty("user.home") + File.separator + ".config";

    public static void persistAgentConfig(List<LogBasicInfo> logBasicInfos) {

        if (logBasicInfos == null || logBasicInfos.size() == 0) {
            return;
        }
        List<String> contents = new ArrayList<String>();
        for (LogBasicInfo logBasicInfo : logBasicInfos) {
            try {
                contents.add(JSON.toJSONString(logBasicInfo));
            } catch (Exception e) {
                logger.error("toJSONString failed! logBasicInfo is " + logBasicInfo, e);
            }
        }

        FileUtils.saveFileContent(configBackPath, contents);
    }

    public static List<LogBasicInfo> loadAgentConfigFromFile() {
        List<String> contents = FileUtils.readFileContent(new File(configBackPath));
        if (contents == null || contents.size() == 0) {
            return null;
        } else {
            List<LogBasicInfo> logBasicInfos = new ArrayList<LogBasicInfo>();
            for (String content : contents) {
                try {
                    logBasicInfos.add(JSON.parseObject(content, LogBasicInfo.class));
                } catch (Exception e) {
                    logger.error("parseObject failed! content is " + content, e);
                }
            }
            return logBasicInfos;
        }
    }

    public static void setConfigBackPath(String configBackPath) {
        AgentConfigPersistence.configBackPath = configBackPath;
    }
}
