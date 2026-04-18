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

import java.util.Map;

/**
 * Interface for Helm3 operations.
 * Mirrors Go's pkg/helm3/helm.go Helm struct methods.
 * Implementation will be provided in task 13.1.
 */
public interface HelmClient {

    /**
     * Pull a chart from the given URL to local cache.
     *
     * @param chartUrl the chart URL/reference
     * @throws Exception if pull fails
     */
    void pullChart(String chartUrl) throws Exception;

    /**
     * Load a chart from local cache.
     *
     * @param chartUrl the chart URL/reference
     * @return an opaque chart object (implementation-specific)
     * @throws Exception if load fails
     */
    Object loadChart(String chartUrl) throws Exception;

    /**
     * Install a chart with the given values.
     *
     * @param chart the chart object returned by {@link #loadChart}
     * @param vals  key-value pairs for chart values
     * @throws Exception if install fails
     */
    void install(Object chart, Map<String, String> vals) throws Exception;

    /**
     * Uninstall the managed release.
     *
     * @throws Exception if uninstall fails
     */
    void uninstall() throws Exception;
}
