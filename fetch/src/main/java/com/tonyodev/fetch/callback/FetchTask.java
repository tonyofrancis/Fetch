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
package com.tonyodev.fetch.callback;

import android.support.annotation.NonNull;

import com.tonyodev.fetch.Fetch;

/**
 * FetchTask is used by Fetch to run short tasks.
 *
 * @author Tonyo Francis
 *
 */
public interface FetchTask {

    /**
     * onProcess runs the short tasks on a
     * worker or UI thread.
     *
     * @param fetch A new instance of fetch. Use this
     *              instance to enqueue requests or query fetch
     *              for download request data. Do not attach
     *              FetchListeners to this instance because they will
     *              be releases once the onProcess method returns.
     * */
    void onProcess(@NonNull Fetch fetch);
}
