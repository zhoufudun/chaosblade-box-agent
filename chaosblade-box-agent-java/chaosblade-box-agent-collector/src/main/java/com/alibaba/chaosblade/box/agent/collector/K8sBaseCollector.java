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

import com.alibaba.chaosblade.box.agent.pkg.FileUtil;
import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.Uri;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base collector for K8s resources with incremental update support.
 * Uses MD5 checksums to detect data changes and ConcurrentHashMap for caching.
 * Mirrors Go's collector/kubernetes/kubernetes.go K8sBaseCollector.
 */
public abstract class K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(K8sBaseCollector.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final String resourceName;
    protected final K8sChannel k8sChannel;
    protected final TransportClient transportClient;
    protected final Uri uri;
    protected final String reportHandler;

    protected final ConcurrentHashMap<String, ResourceIdentifier> identifiers = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, ResourceIdentifier> secondIdentifiers = new ConcurrentHashMap<>();

    protected K8sBaseCollector(String resourceName, K8sChannel k8sChannel,
                               TransportClient transportClient, Uri uri) {
        this.resourceName = resourceName;
        this.k8sChannel = k8sChannel;
        this.transportClient = transportClient;
        this.uri = uri;
        this.reportHandler = uri != null ? uri.getHandlerName() : resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    /**
     * Collect and report resources. Subclasses implement this.
     */
    public abstract void collect();

    /**
     * Check if a resource has changed by comparing MD5 of its data.
     * Returns true if the resource is new or changed.
     */
    protected boolean hasChanged(String uid, Object data, String name) {
        try {
            String md5 = FileUtil.md5sumData(data);
            ResourceIdentifier existing = identifiers.get(uid);
            if (existing != null && md5.equals(existing.getMd5())) {
                existing.setCurr(true);
                return false;
            }
            ResourceIdentifier ri = new ResourceIdentifier(uid, md5, true, name);
            if (existing != null) {
                ri.setCid(existing.getCid());
            }
            identifiers.put(uid, ri);
            return true;
        } catch (Exception e) {
            logger.warn("[{}] md5 check failed for {}: {}", resourceName, uid, e.getMessage());
            return true;
        }
    }

    /**
     * Report K8s metric data to the server.
     */
    @SuppressWarnings("unchecked")
    protected void reportK8sMetric(String namespace, boolean isExists, Object resource, int size) {
        if (size == 0) {
            return;
        }
        try {
            Request request = Request.newDefaultRequest();
            String json = objectMapper.writeValueAsString(resource);
            request.addParam(resourceName, json);

            logger.debug("[{}] reporting {} resources in ns={}", reportHandler, size, namespace);

            Response response = transportClient.invoke(uri, request, true);
            if (!response.isSuccess()) {
                resetIdentifierCache();
                logger.warn("[{}] report failed: {}", reportHandler, response.getError());
                return;
            }

            if (isExists) {
                Object result = response.getResult();
                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    // Handle virtual node special case
                    if (K8sChannel.VIRTUAL_NODE_RESOURCE.equals(reportHandler)) {
                        handleVirtualNodeResponse(resultMap);
                    } else {
                        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                            ResourceIdentifier ri = identifiers.get(entry.getKey());
                            if (ri != null) {
                                ri.setCid(String.valueOf(entry.getValue()));
                            }
                        }
                    }
                }
                logger.info("[{}] report success, ns={}, size={}", reportHandler, namespace, size);
            } else {
                logger.info("[{}] report old resources success, ns={}, size={}", reportHandler, namespace, size);
            }
        } catch (Exception e) {
            resetIdentifierCache();
            logger.warn("[{}] report error: {}", reportHandler, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleVirtualNodeResponse(Map<String, Object> resultMap) {
        Object vnCids = resultMap.get(K8sChannel.VIRTUAL_NODE_RESOURCE);
        if (vnCids instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) vnCids).entrySet()) {
                ResourceIdentifier ri = identifiers.get(entry.getKey());
                if (ri != null) {
                    ri.setCid(String.valueOf(entry.getValue()));
                }
            }
        }
        Object podCids = resultMap.get(K8sChannel.POD_RESOURCE);
        if (podCids instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) podCids).entrySet()) {
                ResourceIdentifier ri = secondIdentifiers.get(entry.getKey());
                if (ri != null) {
                    ri.setCid(String.valueOf(entry.getValue()));
                }
            }
        }
    }

    /**
     * Detect deleted resources (curr=false) and report them with exist=false.
     */
    protected void reportNotExistResource() {
        List<Object> deletedResources = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, ResourceIdentifier> entry : identifiers.entrySet()) {
            ResourceIdentifier ri = entry.getValue();
            if (!ri.isCurr()) {
                CommonInfo info = new CommonInfo();
                info.setUid(ri.getUid());
                info.setExist(false);
                info.setCid(ri.getCid());
                deletedResources.add(info);
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            identifiers.remove(key);
        }

        if (!deletedResources.isEmpty()) {
            reportK8sMetric("", false, deletedResources, deletedResources.size());
        }
    }

    /**
     * Mark all identifiers as not-current before a collection cycle.
     */
    protected void markAllNotCurrent() {
        for (ResourceIdentifier ri : identifiers.values()) {
            ri.setCurr(false);
        }
    }

    /**
     * Reset identifier cache on report failure.
     */
    protected void resetIdentifierCache() {
        identifiers.clear();
        secondIdentifiers.clear();
    }

    /**
     * Get service UID by name from identifiers cache.
     */
    protected String getServiceUidByName(String serviceName) {
        for (Map.Entry<String, ResourceIdentifier> entry : identifiers.entrySet()) {
            if (serviceName.equals(entry.getValue().getName())) {
                return entry.getKey();
            }
        }
        return "";
    }
}
