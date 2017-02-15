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

package com.tonyodev.fetch.listener;


/**
 * Instances of this interface can be attached to instances of
 * Fetch via the addFetchListener method. Listeners will be notified
 * when the status or progress of a download request has changed.
 *
 * @author Tonyo Francis
 * */
public interface FetchListener {
    /**
     * This method is called by an instance of Fetch to notify the listener
     * of status and progress changes of requests managed by the FetchService.
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *         request.
     *
     * @param status download status of a request.
     * @param progress progress/percentage of a request.
     * @param downloadedBytes downloaded file bytes.
     * @param fileSize total file size.
     * @param error error code if the download status is STATUS_ERROR.
     *              Default value is -1(NO ERROR).
     * */
    void onUpdate(long id, int status, int progress, long downloadedBytes, long fileSize, int error);
}