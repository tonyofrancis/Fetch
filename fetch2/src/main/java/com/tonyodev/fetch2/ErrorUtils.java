package com.tonyodev.fetch2;


final class ErrorUtils {

    private ErrorUtils(){}

    static Error getCode(String message) {
        if (message == null) {
            return Error.UNKNOWN;
        }else if(message.equalsIgnoreCase("FNC") || message.equalsIgnoreCase("open failed: ENOENT (No such file or directory)")) {
            return Error.FILE_NOT_CREATED;
        } else if(message.equalsIgnoreCase("TI")) {
            return Error.THREAD_INTERRUPTED;
        }else if(message.equalsIgnoreCase("DIE")) {
            return Error.DOWNLOAD_INTERRUPTED;
        } else if(message.equalsIgnoreCase("recvfrom failed: ETIMEDOUT (Connection timed out)") || message.equalsIgnoreCase("timeout")) {
            return Error.CONNECTION_TIMED_OUT;
        }else if(message.equalsIgnoreCase("java.io.IOException: 404") || message.contains("No address associated getDefaultInstance hostname")) {
            return Error.HTTP_NOT_FOUND;
        }else if(message.contains("Unable to resolve host")){
            return Error.UNKNOWN_HOST;
        } else if(message.equalsIgnoreCase("open failed: EACCES (Permission denied)") || message.equalsIgnoreCase("Permission denied")) {
            return Error.WRITE_PERMISSION_DENIED;
        }else if(message.equalsIgnoreCase("write failed: ENOSPC (No space left onQueued device)")
                || message.equalsIgnoreCase("database or disk is full (code 13)")) {
            return Error.NO_STORAGE_SPACE;
        }else if(message.contains("SSRV:")) {
            return Error.SERVER_ERROR;
        }else if(message.contains("unexpected url:")){
            return Error.BAD_URL;
        }else if(message.equalsIgnoreCase("No such file or directory") || message.equalsIgnoreCase("Attempt to invoke virtual method 'java.lang.String java.io.File.getAbsolutePath()' onQueued a null object reference")) {
            return Error.BAD_FILE_PATH;
        }
        else if(message.equalsIgnoreCase("invalid server response")){
            return Error.INVALID_SERVER_RESPONSE;
        }
        else {
            return Error.UNKNOWN;
        }
    }
}
