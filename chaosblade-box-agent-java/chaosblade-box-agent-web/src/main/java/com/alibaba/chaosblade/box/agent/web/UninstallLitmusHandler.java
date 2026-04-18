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

/**
 * Handles LitmusChaos uninstallation via Helm.
 * Mirrors Go's web/handler/litmuschaos/uninstalllitmus.go UninstallLitmusHandler.
 */
public class UninstallLitmusHandler implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(UninstallLitmusHandler.class);

    private final HelmClient helmClient;

    /**
     * Reference to the LitmusChaosHandler to clear the version on success.
     * May be null if LitmusChaosHandler is not available.
     */
    private final LitmusChaosHandler litmusChaosHandler;

    public UninstallLitmusHandler(HelmClient helmClient, LitmusChaosHandler litmusChaosHandler) {
        this.helmClient = helmClient;
        this.litmusChaosHandler = litmusChaosHandler;
    }

    @Override
    public Response handle(Request request) {
        logger.info("[uninstall litmus] request: {}", request.getParams());
        return uninstallLitmus();
    }

    private Response uninstallLitmus() {
        if (helmClient == null) {
            logger.warn("[uninstall litmus] failed, err: helm instance is nil");
            return Response.returnFail(ErrorCodes.HELM3_EXEC_ERROR, "helm instance is nil");
        }

        try {
            helmClient.uninstall();
        } catch (Exception e) {
            logger.error("[uninstall litmus] uninstall failed! err: {}", e.getMessage());
            return Response.returnFail(ErrorCodes.HELM3_EXEC_ERROR, e.getMessage());
        }

        // Clear version on success
        if (litmusChaosHandler != null) {
            litmusChaosHandler.setLitmusChaosVersion("");
        }

        return Response.returnSuccess();
    }
}
