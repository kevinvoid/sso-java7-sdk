package com.yufu.idaas.sdk.bouncycastle.util.encoders;

/**
 * Created by mac on 2017/1/17.
 */
public class EncoderException extends IllegalStateException {
    private Throwable cause;

    EncoderException(String var1, Throwable var2) {
        super(var1);
        this.cause = var2;
    }

    public Throwable getCause() {
        return this.cause;
    }
}

