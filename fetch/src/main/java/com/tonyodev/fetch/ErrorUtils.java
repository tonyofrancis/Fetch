/*
 * Copyright (C) 2017 Tonyo Francis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tonyodev.fetch;

/**
 *
 * This class holds the error values returned
 * by Fetch or the FetchService when a download request
 * fails or an action against a request fails.
 *
 * @author Tonyo Francis
 */
final class ErrorUtils {

    static final int UNKNOWN = -101;
    static final int FILE_NOT_CREATED = -102;
    static final int THREAD_INTERRUPTED = -103;
    static final int CONNECTION_TIMED_OUT = -104;
    static final int UNKNOWN_HOST = -105;
    static final int HTTP_NOT_FOUND = -106;
    static final int WRITE_PERMISSION_DENIED = -107;
    static final int N0_STORAGE_SPACE = -108;
    static final int ILLEGAL_STATE = -109;
    static final int SERVER_ERROR = -110;
    static final int FILE_NOT_FOUND = -111;
    static final int FILE_ALREADY_CREATED = -112;
    static final int REQUEST_ALREADY_EXIST = - 113;
    static final int INVALID_STATUS = - 114;
    static final int NOT_USABLE = -115;
    static final int BAD_REQUEST = -116;
    static final int ENQUEUE_ERROR = -117;
    static final int DOWNLOAD_INTERRUPTED = -118;

    private ErrorUtils() {
    }

    static int getCode(String message) {

        if (message == null) {
            return UNKNOWN;
        }else if(message.equalsIgnoreCase("FNC") || message.equalsIgnoreCase("open failed: ENOENT (No such file or directory)")) {
            return FILE_NOT_CREATED;
        } else if(message.equalsIgnoreCase("TI")) {
            return THREAD_INTERRUPTED;
        }else if(message.equalsIgnoreCase("DIE")) {
            return DOWNLOAD_INTERRUPTED;
        } else if(message.equalsIgnoreCase("recvfrom failed: ETIMEDOUT (Connection timed out)") || message.equalsIgnoreCase("timeout")) {
            return CONNECTION_TIMED_OUT;
        }else if(message.equalsIgnoreCase("java.io.IOException: 404") || message.contains("No address associated with hostname")) {
            return HTTP_NOT_FOUND;
        }else if(message.contains("Unable to resolve host")){
            return UNKNOWN_HOST;
        } else if(message.equalsIgnoreCase("open failed: EACCES (Permission denied)")) {
            return WRITE_PERMISSION_DENIED;
        }else if(message.equalsIgnoreCase("write failed: ENOSPC (No space left on device)")
                || message.equalsIgnoreCase("database or disk is full (code 13)")) {
            return N0_STORAGE_SPACE;
        }else if(message.contains("SSRV:")) {
            return SERVER_ERROR;
        }
        else if(message.contains("column _file_path is not unique")) {
            return REQUEST_ALREADY_EXIST;
        }
        else {
            return UNKNOWN;
        }
    }
}