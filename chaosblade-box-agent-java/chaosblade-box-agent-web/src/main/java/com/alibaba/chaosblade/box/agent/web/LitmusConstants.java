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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Constants for LitmusChaos operations.
 * Mirrors Go's web/handler/litmuschaos/litmusdefault.go.
 */
public final class LitmusConstants {

    public static final String LITMUS_HELM_NAMESPACE = "chaos";
    public static final String LITMUS_HELM_NAME = "litmuschaos";

    public static final String LITMUS_EXPERIMENT = "experiment";
    public static final String LITMUS_RBAC = "rbac";

    public static final String ENGINE_KIND = "ChaosEngine";
    public static final String K8S_KIND_CLUSTER_ROLE = "ClusterRole";

    public static final String LITMUS_CRD_VERSION = "litmuschaos.io/v1alpha1";

    /** Actions that map to "create" operation. */
    public static final Set<String> CREATE_OPERATIONS = new HashSet<>(Arrays.asList("create", "c"));

    /** Actions that map to "destroy" operation. */
    public static final Set<String> DESTROY_OPERATIONS = new HashSet<>(Arrays.asList("destroy", "d"));

    /** Default namespace when not specified. */
    public static final String DEFAULT_NAMESPACE = "default";

    /** Litmus hub base URL for downloading experiment/RBAC YAML. */
    public static final String LITMUS_HUB_BASE_URL = "https://hub.litmuschaos.io/api/chaos";

    /** Async result polling: initial wait before polling (seconds). */
    public static final int ASYNC_INITIAL_WAIT_SECONDS = 15;

    /** Async result polling: max retry count. */
    public static final int ASYNC_MAX_RETRIES = 90;

    /** Async result polling: wait between retries (seconds). */
    public static final int ASYNC_RETRY_WAIT_SECONDS = 1;

    private LitmusConstants() {
    }
}
