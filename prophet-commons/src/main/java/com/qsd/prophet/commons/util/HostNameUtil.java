package com.qsd.prophet.commons.util;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostNameUtil {
    public static String HOSTNAME;

    static {
        try {
            String hostname = getHostnameByexec();
            if (StringUtils.isNotBlank(hostname)) {
                HOSTNAME = hostname.trim();
            } else {
                HOSTNAME = InetAddress.getLocalHost().getHostName();
            }
        } catch (UnknownHostException e) {
            HOSTNAME = "LocalHost";
        }
    }

    private static String getHostnameByexec() {
        StringBuffer buf = new StringBuffer();
        try {
            Runtime run = Runtime.getRuntime();
            Process proc = run.exec("hostname");
            BufferedInputStream in = new BufferedInputStream(proc.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s;
            while ((s = br.readLine()) != null) {
                buf.append(s);
            }
            String hostname = buf.toString();
            if (StringUtils.isBlank(hostname) || hostname.contains("localhost") || hostname.indexOf("请求超时") != -1) {
                return null;
            }

        } catch (Exception e) {
            return null;
        }
        return buf.toString();
    }

    public static String[] specialCharacters = {"\\\\", "\\^", "\\$", "\\(", "\\)", "\\*", "\\+", "\\?", "\\.", "\\[",
            "\\{", "\\|"};
}
