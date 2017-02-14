package com.qsd.prophet.commons.exception;

/**
 * Created by zhengyu on 2017/2/14.
 */
public class ProphetException extends Exception {
    public ProphetException(Throwable cause) {
        super(cause);
    }

    public ProphetException() {
        super();
    }

    public ProphetException(String message) {
        super(message);
    }

    public ProphetException(String message, Throwable cause) {
        super(message, cause);
    }
}
