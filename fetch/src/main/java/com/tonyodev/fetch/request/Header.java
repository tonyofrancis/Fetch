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
package com.tonyodev.fetch.request;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * HTTP header class that is included with a download request.
 *
 * @author Tonyo Francis
 */

public final class Header {

    private final String header;
    private final String value;

    /**
     * HTTP Header pair
     *
     * @param header HTTP header name
     * @param value HTTP header value
     *
     * @throws NullPointerException if header is null
     * @throws IllegalArgumentException if header contains a ':'
     * */
    public Header(@NonNull String header,@Nullable String value) {

        if (header == null) {
            throw new NullPointerException("header cannot be null");
        }

        if (header.contains(":")) {
            throw new IllegalArgumentException("header may not contain ':'");
        }

        if (value == null) {
            value = "";
        }

        this.header = header;
        this.value = value;
    }

    /**
     * @return HTTP header name
     * */
    @NonNull
    public String getHeader() {
        return header;
    }

    /**
     * @return HTTP header value
     * */
    @NonNull
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return header + ":" + value;
    }
}