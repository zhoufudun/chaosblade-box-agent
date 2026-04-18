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

import java.util.HashMap;
import java.util.Map;

/**
 * API route mapping for all transport URIs.
 * Mirrors Go's transport/transport.go TransportUriMap and transport/request.go constants.
 */
public final class TransportUriMap {

    // --- API name constants ---
    public static final String API_REGISTRY = "registry";
    public static final String API_HEARTBEAT = "heartbeat";
    public static final String API_CLOSE = "close";
    public static final String API_METRIC = "metric";
    public static final String API_CHAOSBLADE_ASYNC = "chaosbladeAsync";
    public static final String API_UPGRADE_CALLBACK = "upgradeCallback";
    public static final String API_JAVA_INSTALL = "javaInstall";
    public static final String API_JAVA_UNINSTALL = "javaUninstall";
    public static final String API_EVENT = "event";

    // K8s resource APIs
    public static final String API_K8S_POD = "k8sPod";
    public static final String API_K8S_VIRTUAL_NODE = "k8sVirtualNode";
    public static final String API_K8S_NODE = "k8sNode";
    public static final String API_K8S_NAMESPACE = "k8sNamespace";
    public static final String API_K8S_SERVICE = "k8sService";
    public static final String API_K8S_DEPLOYMENT = "k8sDeployment";
    public static final String API_K8S_REPLICASET = "k8sReplicaset";
    public static final String API_K8S_INGRESS = "k8sIgress";
    public static final String API_K8S_DAEMONSET = "k8sDaemonset";

    // --- Service name constants ---
    public static final String CHAOS = "Chaos";
    public static final String TOPOLOGY = "Topology";

    // --- Handler path constants ---
    public static final String HTTP_HANDLER_REGISTER = "chaos/AgentRegister";
    public static final String HTTP_HANDLER_HEARTBEAT = "chaos/AgentHeartBeat";
    public static final String HTTP_HANDLER_CLOSE = "chaos/AgentClosed";
    public static final String HTTP_HANDLER_METRIC = "chaos/AgentMetric";
    public static final String MK_CHAOSBLADE_ASYNC = "chaos/chaosbladeAsync";
    public static final String HTTP_HANDLER_JAVA_AGENT_INSTALL = "chaos/javaAgentInstall";
    public static final String HTTP_HANDLER_JAVA_AGENT_UNINSTALL = "chaos/javaAgentUninstall";
    public static final String HTTP_HANDLER_AGENT_EVENT = "chaos/AgentEvent";
    public static final String HTTP_HANDLER_K8S_VIRTUAL_NODE = "chaos/k8sVirtualNode";
    public static final String HTTP_HANDLER_K8S_POD = "chaos/k8sPod";

    private static final Map<String, Uri> uriMap = new HashMap<>();

    private TransportUriMap() {
    }

    /**
     * Initialize all API route mappings with agent context.
     *
     * @param vpcId       VPC ID
     * @param ip          agent IP
     * @param pid         agent PID
     * @param programName program name / tag
     */
    public static void init(String vpcId, String ip, String pid, String programName) {
        uriMap.clear();

        uriMap.put(API_REGISTRY, Uri.newUri(CHAOS, HTTP_HANDLER_REGISTER, vpcId, ip, pid, programName));
        uriMap.put(API_HEARTBEAT, Uri.newUri(CHAOS, HTTP_HANDLER_HEARTBEAT, vpcId, ip, pid, programName));
        uriMap.put(API_CLOSE, Uri.newUri(CHAOS, HTTP_HANDLER_CLOSE, vpcId, ip, pid, programName));
        uriMap.put(API_METRIC, Uri.newUri(CHAOS, HTTP_HANDLER_METRIC, vpcId, ip, pid, programName));
        uriMap.put(API_CHAOSBLADE_ASYNC, Uri.newUri(CHAOS, MK_CHAOSBLADE_ASYNC, vpcId, ip, pid, programName));
        uriMap.put(API_UPGRADE_CALLBACK, Uri.newUri(CHAOS, HTTP_HANDLER_REGISTER, vpcId, ip, pid, programName));
        uriMap.put(API_JAVA_INSTALL, Uri.newUri(CHAOS, HTTP_HANDLER_JAVA_AGENT_INSTALL, vpcId, ip, pid, programName));
        uriMap.put(API_JAVA_UNINSTALL, Uri.newUri(CHAOS, HTTP_HANDLER_JAVA_AGENT_UNINSTALL, vpcId, ip, pid, programName));
        uriMap.put(API_EVENT, Uri.newUri(CHAOS, HTTP_HANDLER_AGENT_EVENT, vpcId, ip, pid, programName));

        uriMap.put(API_K8S_POD, Uri.newUri(CHAOS, HTTP_HANDLER_K8S_POD, vpcId, ip, pid, programName));
        uriMap.put(API_K8S_VIRTUAL_NODE, Uri.newUri(CHAOS, HTTP_HANDLER_K8S_VIRTUAL_NODE, vpcId, ip, pid, programName));
        uriMap.put(API_K8S_NODE, Uri.newUri(TOPOLOGY, "k8sNode", vpcId, ip, pid, programName));
        uriMap.put(API_K8S_NAMESPACE, Uri.newUri(TOPOLOGY, "k8sNamespace", vpcId, ip, pid, programName));
        uriMap.put(API_K8S_SERVICE, Uri.newUri(TOPOLOGY, "k8sService", vpcId, ip, pid, programName));
        uriMap.put(API_K8S_DEPLOYMENT, Uri.newUri(TOPOLOGY, "k8sDeployment", vpcId, ip, pid, programName));
        uriMap.put(API_K8S_REPLICASET, Uri.newUri(TOPOLOGY, "k8sReplicaSet", vpcId, ip, pid, programName));
        uriMap.put(API_K8S_INGRESS, Uri.newUri(TOPOLOGY, "k8sIngress", vpcId, ip, pid, programName));
        uriMap.put(API_K8S_DAEMONSET, Uri.newUri(TOPOLOGY, "k8sDaemonset", vpcId, ip, pid, programName));
    }

    /**
     * Get a Uri by API name.
     *
     * @param apiName the API name constant
     * @return the Uri, or null if not found
     */
    public static Uri get(String apiName) {
        return uriMap.get(apiName);
    }
}
