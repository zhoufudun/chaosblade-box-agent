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

import com.alibaba.chaosblade.box.agent.pkg.AgentContext;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Transport request object containing headers (metadata) and params (data).
 * Mirrors Go's transport/request.go Request struct.
 */
public class Request {

    // Header key constants
    public static final String FROM_HEADER = "FR";
    public static final String CLIENT = "C";
    public static final String CID = "cid";
    public static final String PID = "pid";
    public static final String UID = "uid";

    // Compress version constants
    public static final int NO_COMPRESS = 1;
    public static final int ALL_COMPRESS = 2;
    public static final int REQUEST_COMPRESS = 3;
    public static final int RESPONSE_COMPRESS = 4;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("params")
    private Map<String, String> params;

    public Request() {
        this.headers = new HashMap<>();
        this.params = new HashMap<>();
    }

    /**
     * Create a new Request pre-populated with agent metadata from AgentContext.
     * Equivalent to Go's transport.NewRequest() which reads from options.Opts.
     *
     * @return a new Request with default headers and params
     */
    public static Request newDefaultRequest() {
        return newRequest(
                AgentContext.getPid(),
                AgentContext.getUid(),
                AgentContext.getCid(),
                AgentContext.getProgramName(),
                AgentContext.getVersion(),
                AgentContext.getChaosbladeVersion(),
                AgentContext.getPort()
        );
    }

    /**
     * Create a new Request pre-populated with agent metadata.
     *
     * @param pid               process ID
     * @param uid               unique ID
     * @param cid               connection ID (may be empty)
     * @param programName       program name (tag)
     * @param version           agent version
     * @param chaosbladeVersion chaosblade version
     * @param port              listening port
     * @return a new Request with default headers and params
     */
    public static Request newRequest(String pid, String uid, String cid,
                                     String programName, String version,
                                     String chaosbladeVersion, String port) {
        Request request = new Request();
        request.addHeader(FROM_HEADER, CLIENT);
        request.addHeader(PID, pid);
        request.addHeader(UID, uid);
        if (cid != null && !cid.isEmpty()) {
            request.addHeader(CID, cid);
        }
        request.addHeader("type", programName);
        request.addHeader("v", version);
        request.addHeader("cbv", chaosbladeVersion);
        request.addParam("port", port);
        return request;
    }

    /**
     * Add a header (metadata) entry.
     */
    public Request addHeader(String key, String value) {
        if (key != null && !key.isEmpty()) {
            this.headers.put(key, value);
        }
        return this;
    }

    /**
     * Add a param (data) entry.
     */
    public Request addParam(String key, String value) {
        if (key != null && !key.isEmpty()) {
            this.params.put(key, value);
        }
        return this;
    }

    /**
     * Merge params and headers into a single body map.
     * Headers override params if keys collide.
     */
    public Map<String, String> getBody() {
        Map<String, String> body = new HashMap<>();
        body.putAll(params);
        body.putAll(headers);
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params != null ? params : new HashMap<>();
    }
}
