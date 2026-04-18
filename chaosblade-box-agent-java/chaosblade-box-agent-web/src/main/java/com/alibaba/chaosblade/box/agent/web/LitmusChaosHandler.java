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

package com.alibaba.chaosblade.box.agent.web;

import com.alibaba.chaosblade.box.agent.collector.K8sChannel;
import com.alibaba.chaosblade.box.agent.conn.AsyncReportHandler;
import com.alibaba.chaosblade.box.agent.pkg.UuidUtil;
import com.alibaba.chaosblade.box.agent.transport.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles LitmusChaos experiment create/destroy operations.
 * Mirrors Go's web/handler/litmuschaos/litmuschaos.go LitmusChaosHandler.
 */
public class LitmusChaosHandler implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(LitmusChaosHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TransportClient transportClient;
    private final K8sChannel k8sChannel;

    /** Supplier for the current LitmusChaos version. */
    private volatile String litmusChaosVersion = "";

    public LitmusChaosHandler(TransportClient transportClient, K8sChannel k8sChannel) {
        this.transportClient = transportClient;
        this.k8sChannel = k8sChannel;
    }

    public String getLitmusChaosVersion() {
        return litmusChaosVersion;
    }

    public void setLitmusChaosVersion(String litmusChaosVersion) {
        this.litmusChaosVersion = litmusChaosVersion;
    }

    @Override
    public Response handle(Request request) {
        Map<String, String> params = request.getParams();
        if (params == null) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "chaosAction");
        }
        String chaosAction = params.get("chaosAction");
        if (chaosAction == null || chaosAction.isEmpty()) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "chaosAction");
        }

        if (LitmusConstants.CREATE_OPERATIONS.contains(chaosAction)) {
            return createParameterAndExec(request);
        } else if (LitmusConstants.DESTROY_OPERATIONS.contains(chaosAction)) {
            return destroyParameterAndExec(request);
        }

        return Response.returnFail(ErrorCodes.SERVER_ERROR,
                "litmus exec failed, no such action: " + chaosAction);
    }

    // ---- Destroy ----

    private Response destroyParameterAndExec(Request request) {
        Map<String, String> params = request.getParams();
        if (params == null) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "name");
        }
        String name = params.get("name");
        if (name == null || name.isEmpty()) {
            logger.warn("[litmus destroy] less parameter: `name`");
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "name");
        }

        String namespace = params.get("namespace");
        if (namespace == null || namespace.isEmpty()) {
            namespace = LitmusConstants.DEFAULT_NAMESPACE;
        }

        return destroyExec(name, namespace);
    }

    private Response destroyExec(String name, String namespace) {
        try {
            KubernetesClient client = k8sChannel.getClient();
            CustomResourceDefinitionContext engineContext = new CustomResourceDefinitionContext.Builder()
                    .withGroup("litmuschaos.io")
                    .withVersion("v1alpha1")
                    .withPlural("chaosengines")
                    .withScope("Namespaced")
                    .build();

            client.genericKubernetesResources(engineContext)
                    .inNamespace(namespace)
                    .withName(name)
                    .delete();
        } catch (Exception e) {
            logger.warn("[litmus destroy] delete engine failed: {}", e.getMessage());
            return Response.returnFail(ErrorCodes.SERVER_ERROR,
                    "litmus destroy engine failed, err: " + e.getMessage());
        }
        return Response.returnSuccess();
    }

    // ---- Create ----

    private Response createParameterAndExec(Request request) {
        String name = UuidUtil.generateUid();
        Map<String, String> params = request.getParams();

        String namespace = params.get("namespace");
        if (namespace == null || namespace.isEmpty()) {
            namespace = LitmusConstants.DEFAULT_NAMESPACE;
        }

        String experimentType = params.get("experimentType");
        if (experimentType == null || experimentType.isEmpty()) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "experimentType");
        }

        String experimentName = params.get("experimentName");
        if (experimentName == null || experimentName.isEmpty()) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "experimentName");
        }

        String appInfoStr = params.get("appInfo");
        if (appInfoStr == null || appInfoStr.isEmpty()) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "appInfo");
        }
        Map<String, String> appInfo;
        try {
            appInfo = objectMapper.readValue(appInfoStr, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Response.returnFail(ErrorCodes.PARAMETER_TYPE_ERROR, "appInfo");
        }

        String componentsStr = params.get("components");
        if (componentsStr == null || componentsStr.isEmpty()) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "components");
        }
        Map<String, String> components;
        try {
            components = objectMapper.readValue(componentsStr, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Response.returnFail(ErrorCodes.PARAMETER_TYPE_ERROR, "components");
        }

        return createExec(experimentType, experimentName, namespace, name, appInfo, components);
    }

    private Response createExec(String experimentType, String experimentName,
                                String namespace, String name,
                                Map<String, String> appInfo, Map<String, String> components) {
        if (litmusChaosVersion == null || litmusChaosVersion.isEmpty()) {
            return Response.returnFail(ErrorCodes.SERVER_ERROR,
                    "litmus operator not installed, please install first");
        }

        try {
            prepareLitmusExperiment(experimentType, experimentName, namespace);
        } catch (Exception e) {
            return Response.returnFail(ErrorCodes.SERVER_ERROR,
                    "litmus prepare experiment failed, err: " + e.getMessage());
        }

        try {
            prepareLitmusRbac(experimentType, experimentName, namespace);
        } catch (Exception e) {
            return Response.returnFail(ErrorCodes.SERVER_ERROR,
                    "litmus prepare rbac failed, err: " + e.getMessage());
        }

        try {
            createEngine(name, namespace, experimentName, appInfo, components);
        } catch (Exception e) {
            return Response.returnFail(ErrorCodes.SERVER_ERROR,
                    "litmus create engine failed, err: " + e.getMessage());
        }

        // Async report inject fault status
        asyncHandlerResultStatus(name, namespace, experimentName);

        return Response.returnSuccess();
    }

    // ---- Async Result Polling ----

    private void asyncHandlerResultStatus(String name, String namespace, String experimentName) {
        Thread thread = new Thread(() -> {
            String status = "Unknown";
            String errorStr = "";
            try {
                TimeUnit.SECONDS.sleep(LitmusConstants.ASYNC_INITIAL_WAIT_SECONDS);

                boolean success = false;
                for (int i = 0; i < LitmusConstants.ASYNC_MAX_RETRIES; i++) {
                    try {
                        Map<String, Object> chaosResult = getChaosResult(name, namespace, experimentName);
                        if (chaosResult != null) {
                            int failedRuns = getFailedRuns(chaosResult);
                            String engineName = getEngineName(chaosResult);
                            if (engineName != null && !engineName.isEmpty() && failedRuns == 0) {
                                success = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("[litmus async] retry {}, err: {}", i, e.getMessage());
                    }
                    TimeUnit.SECONDS.sleep(LitmusConstants.ASYNC_RETRY_WAIT_SECONDS);
                }

                if (success) {
                    status = "Success";
                } else {
                    status = "Error";
                    errorStr = "inject fault failed, err: failed in chaos injection phase";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                status = "Error";
                errorStr = "interrupted";
            } catch (Exception e) {
                status = "Error";
                errorStr = "inject fault failed, err: " + e.getMessage();
            }

            // Report result to server
            try {
                Uri uri = TransportUriMap.get(TransportUriMap.API_CHAOSBLADE_ASYNC);
                if (uri != null) {
                    AsyncReportHandler reporter = new AsyncReportHandler(transportClient);
                    reporter.reportStatus(name, status, errorStr, LitmusConstants.LITMUS_HELM_NAME, uri);
                }
            } catch (Exception e) {
                logger.warn("[litmus async] report status failed: {}", e.getMessage());
            }
        }, "litmus-async-" + name);
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getChaosResult(String name, String namespace, String experimentName) {
        KubernetesClient client = k8sChannel.getClient();
        CustomResourceDefinitionContext resultContext = new CustomResourceDefinitionContext.Builder()
                .withGroup("litmuschaos.io")
                .withVersion("v1alpha1")
                .withPlural("chaosresults")
                .withScope("Namespaced")
                .build();

        String chaosResultName = name + "-" + experimentName;
        Map<String, Object> result = client.genericKubernetesResources(resultContext)
                .inNamespace(namespace)
                .withName(chaosResultName)
                .get()
                .getAdditionalProperties();

        // The GenericKubernetesResource wraps the full resource; we need to navigate the structure
        return result;
    }

    @SuppressWarnings("unchecked")
    private int getFailedRuns(Map<String, Object> chaosResult) {
        try {
            Object statusObj = chaosResult.get("status");
            if (statusObj instanceof Map) {
                Map<String, Object> status = (Map<String, Object>) statusObj;
                Object historyObj = status.get("history");
                if (historyObj instanceof Map) {
                    Map<String, Object> history = (Map<String, Object>) historyObj;
                    Object failedRuns = history.get("failedRuns");
                    if (failedRuns instanceof Number) {
                        return ((Number) failedRuns).intValue();
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("[litmus async] parse failedRuns error: {}", e.getMessage());
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private String getEngineName(Map<String, Object> chaosResult) {
        try {
            Object specObj = chaosResult.get("spec");
            if (specObj instanceof Map) {
                Map<String, Object> spec = (Map<String, Object>) specObj;
                Object engineName = spec.get("engineName");
                return engineName != null ? engineName.toString() : "";
            }
        } catch (Exception e) {
            logger.debug("[litmus async] parse engineName error: {}", e.getMessage());
        }
        return "";
    }

    // ---- Prepare Experiment ----

    private void prepareLitmusExperiment(String experimentType, String experimentName, String namespace) throws Exception {
        byte[] yamlBytes = downloadLitmus(experimentType, experimentName, LitmusConstants.LITMUS_EXPERIMENT);

        KubernetesClient client = k8sChannel.getClient();
        CustomResourceDefinitionContext experimentContext = new CustomResourceDefinitionContext.Builder()
                .withGroup("litmuschaos.io")
                .withVersion("v1alpha1")
                .withPlural("chaosexperiments")
                .withScope("Namespaced")
                .build();

        try {
            client.genericKubernetesResources(experimentContext)
                    .inNamespace(namespace)
                    .load(new ByteArrayInputStream(yamlBytes))
                    .create();
        } catch (Exception e) {
            // Ignore AlreadyExists
            if (isAlreadyExists(e)) {
                logger.info("[litmus] experiment already exists, skipping");
            } else {
                throw e;
            }
        }
    }

    // ---- Prepare RBAC ----

    private void prepareLitmusRbac(String experimentType, String experimentName, String namespace) throws Exception {
        byte[] rbacBytes = downloadLitmus(experimentType, experimentName, LitmusConstants.LITMUS_RBAC);
        String rbacYaml = new String(rbacBytes, "UTF-8");
        String[] resources = rbacYaml.split("---");

        if (resources.length < 4) {
            throw new IllegalStateException("get rbac.yaml failed, expected at least 4 sections");
        }

        KubernetesClient client = k8sChannel.getClient();

        // Section [1]: ServiceAccount
        String saYaml = resources[1].trim();
        if (!saYaml.isEmpty()) {
            try {
                ServiceAccount sa = client.serviceAccounts()
                        .load(new ByteArrayInputStream(saYaml.getBytes("UTF-8")))
                        .item();
                client.serviceAccounts().inNamespace(namespace).create(sa);
            } catch (Exception e) {
                if (!isAlreadyExists(e)) {
                    throw e;
                }
            }
        }

        // Section [2]: Role or ClusterRole
        String roleYaml = resources[2].trim();
        // Section [3]: RoleBinding or ClusterRoleBinding
        String roleBindingYaml = resources[3].trim();

        if (roleYaml.contains("kind: " + LitmusConstants.K8S_KIND_CLUSTER_ROLE)
                || roleYaml.contains("kind: ClusterRole")) {
            createClusterRoleAndBinding(client, roleYaml, roleBindingYaml, namespace);
        } else {
            createRoleAndBinding(client, roleYaml, roleBindingYaml, namespace);
        }
    }

    private void createClusterRoleAndBinding(KubernetesClient client, String roleYaml,
                                              String roleBindingYaml, String namespace) throws Exception {
        // Load ClusterRole
        ClusterRole clusterRole = client.rbac().clusterRoles()
                .load(new ByteArrayInputStream(roleYaml.getBytes("UTF-8")))
                .item();

        // Delete if exists, then recreate
        try {
            ClusterRole existing = client.rbac().clusterRoles().withName(clusterRole.getMetadata().getName()).get();
            if (existing != null) {
                logger.info("[prepare litmus] ClusterRole exists, deleting for recreation");
                client.rbac().clusterRoles().withName(clusterRole.getMetadata().getName()).delete();
            }
        } catch (Exception e) {
            // Not found is fine
            if (!isNotFound(e)) {
                throw e;
            }
        }

        client.rbac().clusterRoles().create(clusterRole);

        // ClusterRoleBinding - ignore AlreadyExists
        try {
            ClusterRoleBinding binding = client.rbac().clusterRoleBindings()
                    .load(new ByteArrayInputStream(roleBindingYaml.getBytes("UTF-8")))
                    .item();
            client.rbac().clusterRoleBindings().create(binding);
        } catch (Exception e) {
            if (!isAlreadyExists(e)) {
                throw e;
            }
            logger.info("[prepare litmus] ClusterRoleBinding already exists");
        }
    }

    private void createRoleAndBinding(KubernetesClient client, String roleYaml,
                                       String roleBindingYaml, String namespace) throws Exception {
        // Load Role
        Role role = client.rbac().roles()
                .load(new ByteArrayInputStream(roleYaml.getBytes("UTF-8")))
                .item();

        // Delete if exists, then recreate
        try {
            Role existing = client.rbac().roles().inNamespace(namespace)
                    .withName(role.getMetadata().getName()).get();
            if (existing != null) {
                logger.info("[prepare litmus] Role exists, deleting for recreation");
                client.rbac().roles().inNamespace(namespace)
                        .withName(role.getMetadata().getName()).delete();
            }
        } catch (Exception e) {
            if (!isNotFound(e)) {
                throw e;
            }
        }

        client.rbac().roles().inNamespace(namespace).create(role);

        // RoleBinding - ignore AlreadyExists
        try {
            RoleBinding binding = client.rbac().roleBindings()
                    .load(new ByteArrayInputStream(roleBindingYaml.getBytes("UTF-8")))
                    .item();
            client.rbac().roleBindings().inNamespace(namespace).create(binding);
        } catch (Exception e) {
            if (!isAlreadyExists(e)) {
                throw e;
            }
            logger.info("[prepare litmus] RoleBinding already exists");
        }
    }

    // ---- Create Engine ----

    private void createEngine(String name, String namespace, String experimentName,
                               Map<String, String> appInfo, Map<String, String> components) throws Exception {
        // Build component ENV list
        List<Map<String, String>> componentEnv = new ArrayList<>();
        for (Map.Entry<String, String> entry : components.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            Map<String, String> envVar = new HashMap<>();
            envVar.put("name", entry.getKey());
            envVar.put("value", entry.getValue());
            componentEnv.add(envVar);
        }

        // Build appinfo map
        Map<String, String> appInfoParam = new HashMap<>();
        for (Map.Entry<String, String> entry : appInfo.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String key = entry.getKey();
            if ("appkind".equals(key) || "appns".equals(key) || "applabel".equals(key)) {
                appInfoParam.put(key, entry.getValue());
            }
        }

        // Build ChaosEngine as a generic map structure
        Map<String, Object> chaosEngine = new LinkedHashMap<>();
        chaosEngine.put("apiVersion", LitmusConstants.LITMUS_CRD_VERSION);
        chaosEngine.put("kind", LitmusConstants.ENGINE_KIND);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", name);
        metadata.put("namespace", namespace);
        chaosEngine.put("metadata", metadata);

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("appinfo", appInfoParam);
        spec.put("chaosServiceAccount", experimentName + "-sa");
        spec.put("jobCleanUpPolicy", "delete");
        spec.put("engineState", "active");

        // Experiments list
        Map<String, Object> experiment = new LinkedHashMap<>();
        experiment.put("name", experimentName);
        Map<String, Object> expSpec = new LinkedHashMap<>();
        Map<String, Object> expComponents = new LinkedHashMap<>();
        expComponents.put("env", componentEnv);
        expSpec.put("components", expComponents);
        experiment.put("spec", expSpec);
        spec.put("experiments", Collections.singletonList(experiment));

        chaosEngine.put("spec", spec);

        // Convert to JSON then load as generic resource
        String engineJson = objectMapper.writeValueAsString(chaosEngine);

        KubernetesClient client = k8sChannel.getClient();
        CustomResourceDefinitionContext engineContext = new CustomResourceDefinitionContext.Builder()
                .withGroup("litmuschaos.io")
                .withVersion("v1alpha1")
                .withPlural("chaosengines")
                .withScope("Namespaced")
                .build();

        client.genericKubernetesResources(engineContext)
                .inNamespace(namespace)
                .load(new ByteArrayInputStream(engineJson.getBytes("UTF-8")))
                .create();
    }

    // ---- Download Litmus YAML ----

    /**
     * Download litmus YAML from hub, with local file caching.
     * URL format: https://hub.litmuschaos.io/api/chaos/{version}?file=charts/{type}/{name}/{objectType}.yaml
     */
    private byte[] downloadLitmus(String experimentType, String experimentName, String objectType) throws Exception {
        return downloadLitmusWithVersion(litmusChaosVersion, experimentType, experimentName, objectType);
    }

    static byte[] downloadLitmusWithVersion(String version, String experimentType,
                                             String experimentName, String objectType) throws Exception {
        // Check local cache first
        String localFilePath = String.format("%s/%s/%s/%s.yaml", version, experimentType, experimentName, objectType);
        File localFile = new File(localFilePath);
        if (localFile.exists() && localFile.length() > 0) {
            return readFileBytes(localFile);
        }

        // Download from hub
        String downloadUrl = String.format("%s/%s?file=charts/%s/%s/%s.yaml",
                LitmusConstants.LITMUS_HUB_BASE_URL, version, experimentType, experimentName, objectType);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException(String.format("download `%s` failed! response code: %d", downloadUrl, responseCode));
            }

            byte[] data;
            try (InputStream in = connection.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                data = baos.toByteArray();
            }

            // Cache locally
            try {
                localFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    fos.write(data);
                }
            } catch (Exception e) {
                logger.warn("[litmus] cache file locally failed: {}", e.getMessage());
            }

            return data;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    // ---- Utility ----

    private static boolean isAlreadyExists(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("AlreadyExists") || msg.contains("already exists"));
    }

    private static boolean isNotFound(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("NotFound") || msg.contains("not found"));
    }
}
