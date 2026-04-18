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
 * Transport URI routing descriptor.
 * Mirrors Go's transport/request.go Uri struct.
 */
public class Uri {

    private String serverName;
    private String handlerName;
    private String vpcId;
    private String ip;
    private String pid;
    private String tag;
    private String requestId;
    private String compressVersion;

    public Uri() {
    }

    public Uri(String serverName, String handlerName, String vpcId, String ip,
               String pid, String tag, String compressVersion) {
        this.serverName = serverName;
        this.handlerName = handlerName;
        this.vpcId = vpcId;
        this.ip = ip;
        this.pid = pid;
        this.tag = tag;
        this.compressVersion = compressVersion;
    }

    /**
     * Create a new Uri with agent context.
     *
     * @param serverName  service name (e.g. "Chaos", "Topology")
     * @param handlerName handler path (e.g. "chaos/AgentRegister")
     * @param vpcId       VPC ID
     * @param ip          agent IP
     * @param pid         agent PID
     * @param programName program name / tag
     * @return a new Uri
     */
    public static Uri newUri(String serverName, String handlerName,
                             String vpcId, String ip, String pid, String programName) {
        return new Uri(serverName, handlerName, vpcId, ip, pid, programName,
                String.valueOf(Request.NO_COMPRESS));
    }

    // --- Getters and Setters ---

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCompressVersion() {
        return compressVersion;
    }

    public void setCompressVersion(String compressVersion) {
        this.compressVersion = compressVersion;
    }
}
