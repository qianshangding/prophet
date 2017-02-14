package com.qsd.prophet.agent.service.dubbo;

import com.qsd.prophet.agent.api.Agent;
import com.qsd.prophet.agent.entity.LogInfo;
import com.qsd.prophet.agent.service.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentImpl implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(AgentImpl.class);

    @Override
    public List<String> getLogInfo(LogInfo logInfo) {
        if (logger.isDebugEnabled()) {
            logger.debug("getLogInfo logInfo is " + logInfo);
        }

        Map<Integer, String> pathMap = logInfo.getPathMap();

        if (pathMap == null || pathMap.isEmpty()) {
            return null;
        }

        List<String> contents = new ArrayList<>();
        for (String absPath : pathMap.values()) {
            contents.addAll(FileUtils.readFileContent(absPath, logInfo.getReadLogSizePerLog()));
        }

        return contents;
    }
}
