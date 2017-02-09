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
import android.support.annotation.Nullable;

import com.tonyodev.fetch.request.Request;

/**
 * FetchCall is used by Fetch as a callback
 * when Fetch.Call(Request,FetchCall) requests are made.
 *
 * @author Tonyo Francis
 */
public interface FetchCall<T> {

    void onSuccess(@Nullable T response, @NonNull Request request);
    void onError(int error,@NonNull Request request);
}