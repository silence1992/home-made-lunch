package com.homemadelunch.common;

/**
 * 业务异常
 */
public class BizException extends RuntimeException {

    private int code;

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
