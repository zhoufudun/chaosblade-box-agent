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

import com.alibaba.chaosblade.box.agent.pkg.AuthUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Authentication interceptor: validates signatures on inbound requests
 * and adds AK/signature on outbound requests.
 * Mirrors Go's transport/interceptor.go authInterceptor.
 */
public class AuthInterceptor implements RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String SIGN_DATA = "sd";
    public static final String ACCESS_KEY = "ak";
    public static final String SIGN_KEY = "sn";

    @Override
    public Response handle(Request request) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            return Response.returnFail(ErrorCodes.FORBIDDEN, "missing headers");
        }

        // Check sign exists
        String sign = headers.get(SIGN_KEY);
        if (sign == null || sign.isEmpty()) {
            return Response.returnFail(ErrorCodes.FORBIDDEN, "missing sign");
        }

        // Check access key matches local
        String accessKey = headers.get(ACCESS_KEY);
        if (accessKey != null && !accessKey.isEmpty()
                && !accessKey.equals(AuthUtil.getAccessKey())) {
            return Response.returnFail(ErrorCodes.FORBIDDEN, "accessKey not matched");
        }

        // Determine sign data
        String signData = headers.get(SIGN_DATA);
        if (signData == null || signData.isEmpty()) {
            try {
                signData = objectMapper.writeValueAsString(request.getParams());
            } catch (JsonProcessingException e) {
                return Response.returnFail(ErrorCodes.FORBIDDEN, "invalid request parameters");
            }
        }

        // Verify signature
        if (!AuthUtil.auth(sign, signData)) {
            return Response.returnFail(ErrorCodes.FORBIDDEN, "illegal request");
        }

        return null;
    }

    @Override
    public Response invoke(Request request) {
        String accessKey = AuthUtil.getAccessKey();
        String secureKey = AuthUtil.getSecureKey();
        if (accessKey == null || accessKey.isEmpty()
                || secureKey == null || secureKey.isEmpty()) {
            return Response.returnFail(ErrorCodes.TOKEN_NOT_FOUND);
        }

        request.addHeader(ACCESS_KEY, accessKey);

        // Compute sign data
        Map<String, String> headers = request.getHeaders();
        String signData = headers != null ? headers.get(SIGN_DATA) : null;
        if (signData == null || signData.isEmpty()) {
            try {
                signData = objectMapper.writeValueAsString(request.getParams());
            } catch (JsonProcessingException e) {
                return Response.returnFail(ErrorCodes.ENCODE_ERROR, e.getMessage());
            }
        }

        String sign = AuthUtil.sign(signData);
        request.addHeader(SIGN_KEY, sign);

        return null;
    }
}
