package com.yufu.idaas.sdk.exception;

/**
 * Created by shuowang on 2018/5/2.
 */
public class YufuInitException extends BaseYufuException {
    public YufuInitException(String msg) {
        super("Failed to initiate with reason: " + msg);
    }
}
