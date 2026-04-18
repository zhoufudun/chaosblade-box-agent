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

import com.alibaba.chaosblade.box.agent.pkg.UuidUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main transport client that integrates the channel and interceptor chain.
 * Mirrors Go's transport/transport.go TransportClient.
 */
public class TransportClient {

    private static final Logger logger = LoggerFactory.getLogger(TransportClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TransportChannel channel;
    private final RequestInterceptor interceptor;

    public TransportClient(TransportChannel channel, RequestInterceptor interceptor) {
        this.channel = channel;
        this.interceptor = interceptor;
    }

    public TransportClient(TransportChannel channel) {
        this(channel, InterceptorChain.buildInterceptor());
    }

    /**
     * Invoke a remote request.
     *
     * @param uri             the routing URI
     * @param request         the request to send
     * @param needInterceptor whether to run the interceptor chain
     * @return the deserialized Response
     * @throws IOException if the request fails
     */
    public Response invoke(Uri uri, Request request, boolean needInterceptor) throws IOException {
        // Run interceptor chain for outbound enrichment
        if (needInterceptor && interceptor != null) {
            Response interceptResponse = interceptor.invoke(request);
            if (interceptResponse != null) {
                return interceptResponse;
            }
        }

        // Generate request ID
        String requestId = UuidUtil.getUUID();
        request.addHeader("rid", requestId);
        uri.setRequestId(requestId);

        // Serialize request to JSON
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            logger.warn("Marshal request to json error for {}/{}: {}",
                    uri.getServerName(), uri.getHandlerName(), e.getMessage());
            throw new IOException("Failed to serialize request", e);
        }

        // Send via channel
        String result;
        try {
            result = channel.doInvoker(uri, json);
        } catch (IOException e) {
            logger.warn("Invoke failed for requestId {}: {}", requestId, e.getMessage());
            throw e;
        }

        // Deserialize response
        try {
            return objectMapper.readValue(result, Response.class);
        } catch (Exception e) {
            logger.warn("Unmarshal response failed for requestId {}: {}", requestId, e.getMessage());
            throw new IOException("Failed to deserialize response", e);
        }
    }
}
