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

package com.alibaba.chaosblade.box.agent.conn;

import com.alibaba.chaosblade.box.agent.pkg.AgentContext;
import com.alibaba.chaosblade.box.agent.pkg.AuthUtil;
import com.alibaba.chaosblade.box.agent.pkg.EnvUtil;
import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import com.alibaba.chaosblade.box.agent.transport.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Registration handler. Sends agent registration request to the server.
 * Mirrors Go's conn/connect/connect.go ClientConnectHandler.
 */
public class ConnectHandler implements ClientHandle {

    private static final Logger logger = LoggerFactory.getLogger(ConnectHandler.class);

    /** Device type constant for host mode (matches Go's options.Host = 0). */
    private static final String DEVICE_TYPE_HOST = "0";

    private final TransportClient transportClient;
    private final ConnConfig config;

    public ConnectHandler(TransportClient transportClient, ConnConfig config) {
        this.transportClient = transportClient;
        this.config = config;
    }

    @Override
    public void start() throws Exception {
        // Use newRequest to include default headers (FR=C, pid, uid, type, v, cbv, port)
        // matching Go's transport.NewRequest() behavior
        Request request = Request.newRequest(
                config.getPid(),
                config.getUid(),
                config.getCid(),
                "CHAOS_AGENT",
                config.getVersion(),
                config.getChaosbladeVersion(),
                config.getPort()
        );
        request.addParam("ip", config.getIp());
        request.addParam("pid", config.getPid());
        request.addParam("type", "CHAOS_AGENT");
        request.addParam("uid", config.getUid());
        request.addParam("instanceId", config.getInstanceId());
        request.addParam("namespace", config.getNamespace());
        request.addParam("deviceId", config.getInstanceId());
        request.addParam("deviceType", DEVICE_TYPE_HOST);
        request.addParam("ak", config.getLicense());
        request.addParam("uptime", EnvUtil.getUptime());
        request.addParam("startupMode", config.getStartupMode());
        request.addParam("v", config.getVersion());
        request.addParam("agentMode", config.getMode());
        request.addParam("osType", config.getInstallOperator());
        request.addParam("cpuNum", String.valueOf(Runtime.getRuntime().availableProcessors()));
        request.addParam("clusterId", config.getClusterId());
        request.addParam("clusterName", config.getClusterName());

        String cbv = config.getChaosbladeVersion();
        if (cbv != null && !cbv.isEmpty()) {
            request.addParam("cbv", cbv);
        }

        request.addParam("memSize", String.valueOf(Runtime.getRuntime().maxMemory()));

        request.addParam("appInstance", config.getApplicationInstance());
        request.addParam("appGroup", config.getApplicationGroup());

        if (config.isRestrictedVpc()) {
            request.addParam("restrictedVpc", "true");
            request.addParam("vpcId", config.getLicense());
        } else {
            request.addParam("vpcId", config.getVpcId());
        }

        Uri uri = TransportUriMap.get(TransportUriMap.API_REGISTRY);
        Response response = transportClient.invoke(uri, request, false);
        handleConnectResponse(response);
    }

    @Override
    public void stop() {
        // no-op
    }

    @SuppressWarnings("unchecked")
    private void handleConnectResponse(Response response) throws Exception {
        if (!response.isSuccess()) {
            throw new RuntimeException("Connect server failed: " + response.getError());
        }

        Object result = response.getResult();
        if (!(result instanceof Map)) {
            throw new RuntimeException("Response result is not a map");
        }

        Map<String, Object> resultMap = (Map<String, Object>) result;

        Object cidObj = resultMap.get("cid");
        if (cidObj != null) {
            config.setCid(String.valueOf(cidObj));
            AgentContext.setCid(String.valueOf(cidObj));
        }

        Object uidObj = resultMap.get("uid");
        if (uidObj != null) {
            config.setUid(String.valueOf(uidObj));
            AgentContext.setUid(String.valueOf(uidObj));
        }

        Object akObj = resultMap.get("ak");
        Object skObj = resultMap.get("sk");
        if (akObj == null || skObj == null) {
            logger.error("Response data is wrong, lack ak or sk");
            throw new RuntimeException("accessKey or secretKey is empty");
        }

        AuthUtil.recordSecretKeyToFile(String.valueOf(akObj), String.valueOf(skObj));
        logger.info("[connect] registration successful, cid={}", config.getCid());
    }
}
