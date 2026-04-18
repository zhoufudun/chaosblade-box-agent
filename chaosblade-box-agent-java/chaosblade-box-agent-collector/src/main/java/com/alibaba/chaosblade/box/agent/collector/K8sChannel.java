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

package com.alibaba.chaosblade.box.agent.collector;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton Kubernetes client channel.
 * Uses fabric8 InClusterConfig for auto-detection.
 * Mirrors Go's pkg/kubernetes/kubernetes.go Channel.
 */
public class K8sChannel {

    private static final Logger logger = LoggerFactory.getLogger(K8sChannel.class);

    public static final String POD_RESOURCE = "pods";
    public static final String SERVICE_RESOURCE = "services";
    public static final String DEPLOYMENT_RESOURCE = "deployments";
    public static final String DAEMONSET_RESOURCE = "daemonsets";
    public static final String NAMESPACE_RESOURCE = "namespaces";
    public static final String REPLICASET_RESOURCE = "replicasets";
    public static final String NODE_RESOURCE = "nodes";
    public static final String INGRESS_RESOURCE = "ingresses";
    public static final String VIRTUAL_NODE_RESOURCE = "virtualNodes";

    private static volatile K8sChannel instance;
    private static final Object LOCK = new Object();

    private final KubernetesClient client;
    private String clusterId;

    private K8sChannel(KubernetesClient client, String clusterId) {
        this.client = client;
        this.clusterId = clusterId;
    }

    /**
     * Get or create the singleton K8sChannel.
     * Returns null if not running inside a K8s cluster.
     */
    public static K8sChannel getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (LOCK) {
            if (instance == null) {
                try {
                    Config config = Config.autoConfigure(null);
                    KubernetesClient client = new DefaultKubernetesClient(config);
                    String host = config.getMasterUrl();
                    String clusterId = "default-cluster";
                    if (host != null && !host.isEmpty()) {
                        clusterId = "API_SERVER_" + host;
                    }
                    instance = new K8sChannel(client, clusterId);
                    logger.info("[K8sChannel] initialized, clusterId={}", clusterId);
                } catch (Exception e) {
                    logger.warn("[K8sChannel] create k8s client failed: {}", e.getMessage());
                    return null;
                }
            }
        }
        return instance;
    }

    /**
     * Create a K8sChannel with a provided client (for testing).
     */
    public static K8sChannel createWithClient(KubernetesClient client, String clusterId) {
        return new K8sChannel(client, clusterId);
    }

    public KubernetesClient getClient() {
        return client;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    /** Reset singleton for testing. */
    static void resetInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
