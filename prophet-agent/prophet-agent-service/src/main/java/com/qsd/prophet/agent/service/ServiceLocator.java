package com.qsd.prophet.agent.service;

import com.qsd.prophet.agent.service.util.ConfigManageBean;
import com.qsd.prophet.config.api.Config;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ServiceLocator implements ApplicationContextAware {

    private static ApplicationContext context;

    public static HeartbeatOperator getHeartbeatOperator() {
        return (HeartbeatOperator) context.getBean("heartbeatOperator");
    }

//    public static AgentMqProducer getAgentMqProducer() {
//        return (AgentMqProducer) context.getBean("agentMqProducer");
//    }
//
//    public static AgentKafkaProducer getAgentKafkaProducer() {
//        return (AgentKafkaProducer) context.getBean("agentKafkaProducer");
//    }
//
//    public static AgentFlumeClient getAgentFlumeClient() {
//        return (AgentFlumeClient) context.getBean("agentFlumeClient");
//    }

    public static Config getConfigService() {
        return (Config) context.getBean("configService");
    }

    public static ConfigManageBean getConfigManageBean() {
        return (ConfigManageBean) context.getBean("configManageBean");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
