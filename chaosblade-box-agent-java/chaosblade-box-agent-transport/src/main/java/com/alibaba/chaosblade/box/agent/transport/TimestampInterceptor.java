/*
 * Copyright 2025 The ChaosBlade Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.chaosblade.box.agent.transport;

import java.util.Map;

/**
 * Timestamp interceptor: validates timestamp on inbound requests
 * and adds current timestamp on outbound requests.
 * Mirrors Go's transport/interceptor.go timestampInterceptor.
 */
public class TimestampInterceptor implements RequestInterceptor {

    public static final String TIMESTAMP_KEY = "ts";

    @Override
    public Response handle(Request request) {
        Map<String, String> params = request.getParams();
        if (params == null) {
            return Response.returnFail(ErrorCodes.INVALID_TIMESTAMP);
        }
        String requestTime = params.get(TIMESTAMP_KEY);
        if (requestTime == null || requestTime.isEmpty()) {
            return Response.returnFail(ErrorCodes.INVALID_TIMESTAMP);
        }
        try {
            Long.parseLong(requestTime);
        } catch (NumberFormatException e) {
            return Response.returnFail(ErrorCodes.INVALID_TIMESTAMP);
        }
        return null;
    }

    @Override
    public Response invoke(Request request) {
        long currentTimeMillis = System.currentTimeMillis();
        request.addParam(TIMESTAMP_KEY, String.valueOf(currentTimeMillis));
        return null;
    }
}
