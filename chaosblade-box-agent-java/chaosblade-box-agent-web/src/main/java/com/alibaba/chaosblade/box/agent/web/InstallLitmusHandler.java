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

import com.alibaba.chaosblade.box.agent.transport.ErrorCodes;
import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles LitmusChaos installation via Helm.
 * Mirrors Go's web/handler/litmuschaos/installlitmus.go InstallLitmusHandler.
 */
public class InstallLitmusHandler implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(InstallLitmusHandler.class);

    private final HelmClient helmClient;
    private final String litmusChartUrl;

    /**
     * Reference to the LitmusChaosHandler to update the version on success.
     * May be null if LitmusChaosHandler is not available.
     */
    private final LitmusChaosHandler litmusChaosHandler;

    public InstallLitmusHandler(HelmClient helmClient, String litmusChartUrl,
                                 LitmusChaosHandler litmusChaosHandler) {
        this.helmClient = helmClient;
        this.litmusChartUrl = litmusChartUrl;
        this.litmusChaosHandler = litmusChaosHandler;
    }

    @Override
    public Response handle(Request request) {
        Map<String, String> params = request.getParams();
        if (params == null) {
            return Response.returnFail(ErrorCodes.PARAMETER_LESS, "version");
        }
        logger.info("[install litmus] request: {}", params);

        String version = params.get("version");
        if (version == null || version.isEmpty()) {
            return Response.returnFail(ErrorCodes.PARAMETER_LESS, "version");
        }

        Map<String, String> vals = new HashMap<>();
        vals.put("namespace", LitmusConstants.LITMUS_HELM_NAMESPACE);

        return installLitmus(version, vals);
    }

    private Response installLitmus(String version, Map<String, String> vals) {
        if (helmClient == null) {
            logger.warn("[install litmus] failed, err: helm instance is nil");
            return Response.returnFail(ErrorCodes.HELM3_EXEC_ERROR, "helm instance is nil");
        }

        String chartUrl = getLitmusUrlByVersion(version);

        // Pull chart to cache
        try {
            helmClient.pullChart(chartUrl);
        } catch (Exception e) {
            logger.warn("[install litmus] pull chart failed! err: {}", e.getMessage());
            return Response.returnFail(ErrorCodes.HELM3_EXEC_ERROR, e.getMessage());
        }

        // Load chart
        Object chart;
        try {
            chart = helmClient.loadChart(chartUrl);
        } catch (Exception e) {
            logger.warn("[install litmus] load chart by url `{}`, failed! err: {}", chartUrl, e.getMessage());
            return Response.returnFail(ErrorCodes.HELM3_EXEC_ERROR, e.getMessage());
        }

        // Install chart
        try {
            helmClient.install(chart, vals);
        } catch (Exception e) {
            logger.warn("[install litmus] install failed, err: {}", e.getMessage());
            return Response.returnFail(ErrorCodes.HELM3_EXEC_ERROR, e.getMessage());
        }

        // Update version on success
        if (litmusChaosHandler != null) {
            litmusChaosHandler.setLitmusChaosVersion(version);
        }

        return Response.returnSuccess();
    }

    private String getLitmusUrlByVersion(String version) {
        return litmusChartUrl + ":" + version;
    }
}
