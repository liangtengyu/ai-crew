package com.aicrew.common.exception;

/** 业务异常：消息直接透出给前端 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
