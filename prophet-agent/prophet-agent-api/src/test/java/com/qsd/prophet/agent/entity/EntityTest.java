package com.qsd.prophet.agent.entity;

import org.junit.Test;

public class EntityTest {
    @Test
    public void testCmdInfo() {
        CmdResult cmdResult = new CmdResult();
        cmdResult.setExceptionInfo("");
        cmdResult.setHost("127.0.0.1");
        cmdResult.setResult(200);
        cmdResult.setResultDetail("ResultDetail");
        cmdResult.setScriptFile("run.bat");
        System.out.println(cmdResult.toString());
    }
}
