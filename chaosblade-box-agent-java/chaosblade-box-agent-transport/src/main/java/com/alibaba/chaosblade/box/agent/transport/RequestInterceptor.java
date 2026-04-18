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

/**
 * Request interceptor interface for inbound validation and outbound enrichment.
 * Mirrors Go's transport/interceptor.go RequestInterceptor interface.
 *
 * <p>Both methods return null if the request passes validation,
 * or a Response with an error if validation fails.</p>
 */
public interface RequestInterceptor {

    /**
     * Handle inbound request validation.
     *
     * @param request the inbound request
     * @return null if passed, or a failure Response
     */
    Response handle(Request request);

    /**
     * Invoke outbound request enrichment.
     *
     * @param request the outbound request
     * @return null if passed, or a failure Response
     */
    Response invoke(Request request);
}
