package com.qsd.prophet.commons.constant;

public enum LogType {
    SERVICE("service", 1), SAL("sal", 2), DAL("dal", 4), ERROR("error", 8), CUSTOM("custom", 16);

    private String name;
    private int value;

    private LogType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static boolean isLogType(int value, LogType logType) {
        return (logType.getValue() & value) == logType.getValue();
    }

    public static boolean isAppLog(int value) {
        return (value & 15) > 0;
    }

    public static LogType getLogType(String name) {
        for (LogType logType : LogType.values()) {
            if (logType.getName().equals(name)) {
                return logType;
            }
        }
        return null;
    }
}
