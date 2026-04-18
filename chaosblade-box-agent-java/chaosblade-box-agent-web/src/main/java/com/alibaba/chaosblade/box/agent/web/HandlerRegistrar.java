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
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registers all API handlers with the AgentController.
 * Called from the bootstrap module during startup.
 * Mirrors Go's handler registration in web/server/http.go.
 */
public final class HandlerRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(HandlerRegistrar.class);

    private HandlerRegistrar() {
        // utility class
    }

    /**
     * Register all API handlers with the given controller.
     *
     * @param controller       the AgentController to register handlers on
     * @param transportClient  transport client for handlers that need server communication
     * @param k8sChannel       Kubernetes channel (may be null if not in K8s mode)
     * @param helmClient       Helm client (may be null if Helm is not available)
     * @param bladeBinPath     path to the blade CLI binary
     * @param litmusChartUrl   URL for the LitmusChaos Helm chart
     * @param ctlPathSupplier  supplier for the chaosctl.sh file path
     * @param optionsUpdater   callback to update application instance/group in global options
     */
    public static void registerAll(AgentController controller,
                                   TransportClient transportClient,
                                   K8sChannel k8sChannel,
                                   HelmClient helmClient,
                                   String bladeBinPath,
                                   String litmusChartUrl,
                                   Supplier<String> ctlPathSupplier,
                                   Consumer<String[]> optionsUpdater) {

        // Simple handlers (no external dependencies)
        controller.registerHandler("ping", new ServerRequestHandler(new PingHandler()));
        controller.registerHandler("uninstall", new ServerRequestHandler(new UninstallHandler(ctlPathSupplier)));
        controller.registerHandler("updateApplication", new ServerRequestHandler(new UpdateApplicationHandler(optionsUpdater)));

        // ChaosbladeHandler needs transport client and blade binary path
        controller.registerHandler("chaosblade", new ServerRequestHandler(
                new ChaosbladeHandler(transportClient, bladeBinPath)));

        // LitmusChaos handlers share a LitmusChaosHandler reference
        LitmusChaosHandler litmusChaosHandler = new LitmusChaosHandler(transportClient, k8sChannel);
        controller.registerHandler("litmuschaos", new ServerRequestHandler(litmusChaosHandler));
        controller.registerHandler("installLitmus", new ServerRequestHandler(
                new InstallLitmusHandler(helmClient, litmusChartUrl, litmusChaosHandler)));
        controller.registerHandler("uninstallLitmus", new ServerRequestHandler(
                new UninstallLitmusHandler(helmClient, litmusChaosHandler)));

        logger.info("[HandlerRegistrar] all handlers registered");
    }
}
