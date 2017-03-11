package com.tonyodev.fetch.exception;

/**
 * Created by tonyofrancis on 3/11/17.
 */

public class DownloadInterruptedException extends RuntimeException {
    private int errorCode;

    public DownloadInterruptedException(String message,int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
