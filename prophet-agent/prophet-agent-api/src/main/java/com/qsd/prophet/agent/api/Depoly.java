package com.qsd.prophet.agent.api;

import com.qsd.prophet.agent.entity.CmdResult;
import com.qsd.prophet.agent.entity.DeployInfo;
import com.qsd.prophet.agent.entity.DeployResult;

import javax.activation.CommandInfo;

/**
 * Created by zhengyu on 2017/2/14.
 */
public interface Depoly {
    /**
     * 根据部署信息，部署软件包
     *
     * @param deployInfo
     * @return
     */
    public DeployResult deployApp(DeployInfo deployInfo);

    public CmdResult executeCommand(CommandInfo commandInfo);

    public String getSoftPackageMd5(DeployInfo deployInfo);
}
