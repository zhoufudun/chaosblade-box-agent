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

import java.util.ArrayList;
import java.util.List;

/**
 * Chains multiple {@link RequestInterceptor} instances.
 * Chain order: timestamp → auth (timestamp first for both handle and invoke).
 * Mirrors Go's transport/interceptor.go BuildInterceptor / requestInterceptorChain.
 */
public class InterceptorChain implements RequestInterceptor {

    private final List<RequestInterceptor> interceptors;

    public InterceptorChain(List<RequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * Handle inbound: calls each interceptor's handle in order, stops on first failure.
     */
    @Override
    public Response handle(Request request) {
        for (RequestInterceptor interceptor : interceptors) {
            Response response = interceptor.handle(request);
            if (response != null) {
                return response;
            }
        }
        return null;
    }

    /**
     * Invoke outbound: calls each interceptor's invoke in order, stops on first failure.
     */
    @Override
    public Response invoke(Request request) {
        for (RequestInterceptor interceptor : interceptors) {
            Response response = interceptor.invoke(request);
            if (response != null) {
                return response;
            }
        }
        return null;
    }

    /**
     * Build the default interceptor chain: timestamp → auth.
     */
    public static InterceptorChain buildInterceptor() {
        List<RequestInterceptor> chain = new ArrayList<>();
        chain.add(new TimestampInterceptor());
        chain.add(new AuthInterceptor());
        return new InterceptorChain(chain);
    }
}
