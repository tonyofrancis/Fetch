package com.tonyodev.fetch2;

import android.support.annotation.NonNull;

public enum Status {
    ERROR(-1),
    CANCELLED(0),
    QUEUED(1),
    DOWNLOADING(2),
    PAUSED(3),
    COMPLETED(4),
    REMOVED(5),
    INVALID(6);

    private int value;

    Status(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    public static Status valueOf(int status) {
        switch (status) {
            case -1:return ERROR;
            case 0:return CANCELLED;
            case 1:return QUEUED;
            case 2:return DOWNLOADING;
            case 3:return PAUSED;
            case 4:return COMPLETED;
            case 5:return REMOVED;
            default:return INVALID;
        }
    }

    @Override
    public String toString() {
        return "Status: " + value;
    }
}