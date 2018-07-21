package com.yufu.idaas.sdk.bouncycastle.openssl;

import java.io.IOException;

/**
 * Created by mac on 2017/1/18.
 */
public class PEMException extends IOException {
    Exception underlying;

    public PEMException(String var1) {
        super(var1);
    }

    public PEMException(String var1, Exception var2) {
        super(var1);
        this.underlying = var2;
    }

    public Exception getUnderlyingException() {
        return this.underlying;
    }

    public Throwable getCause() {
        return this.underlying;
    }
}
