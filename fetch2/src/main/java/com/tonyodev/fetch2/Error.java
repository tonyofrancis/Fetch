package com.tonyodev.fetch2;

import android.support.annotation.NonNull;

public enum Error {
    UNKNOWN(-1),
    NONE(0),
    REQUEST_ALREADY_EXIST(1),
    FILE_NOT_CREATED (2),
    THREAD_INTERRUPTED (3),
    DOWNLOAD_INTERRUPTED (4),
    CONNECTION_TIMED_OUT (5),
    HTTP_NOT_FOUND (6),
    UNKNOWN_HOST (7),
    WRITE_PERMISSION_DENIED (8),
    NO_STORAGE_SPACE(9),
    SERVER_ERROR (10),
    UNSUCCESSFUL_CONNECTION (11),
    NO_NETWORK_CONNECTION (12),
    BAD_URL (13),
    BAD_FILE_PATH (14),
    INVALID_SERVER_RESPONSE(15),
    REQUEST_NOT_FOUND_IN_DATABASE(16);

    private int value;

    Error(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    public static Error valueOf(int error) {
        switch (error) {
            case -1: return UNKNOWN;
            case 0: return NONE;
            case 1: return REQUEST_ALREADY_EXIST;
            case 2: return FILE_NOT_CREATED;
            case 3: return THREAD_INTERRUPTED;
            case 4: return DOWNLOAD_INTERRUPTED;
            case 5: return CONNECTION_TIMED_OUT;
            case 6: return HTTP_NOT_FOUND;
            case 7: return UNKNOWN_HOST;
            case 8: return WRITE_PERMISSION_DENIED;
            case 9: return NO_STORAGE_SPACE;
            case 10:return SERVER_ERROR;
            case 11:return UNSUCCESSFUL_CONNECTION;
            case 12:return NO_NETWORK_CONNECTION;
            case 13:return BAD_URL;
            case 14:return BAD_FILE_PATH;
            case 15:return INVALID_SERVER_RESPONSE;
            case 16:return REQUEST_NOT_FOUND_IN_DATABASE;
            default:return UNKNOWN;
        }
    }
    
    @Override
    public String toString() {
        return "Error Code: " + value;
    }
}