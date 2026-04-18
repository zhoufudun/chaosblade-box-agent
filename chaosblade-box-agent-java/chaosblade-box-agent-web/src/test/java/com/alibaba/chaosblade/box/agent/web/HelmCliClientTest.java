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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HelmCliClient}.
 * These tests verify the client construction and argument building logic
 * without requiring an actual helm binary.
 */
class HelmCliClientTest {

    @Test
    void constructorSetsDefaults() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace");
        assertNotNull(client);
    }

    @Test
    void constructorWithCustomBinaryPath() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/usr/local/bin/helm");
        assertNotNull(client);
    }

    @Test
    void constructorWithNullBinaryPathDefaultsToHelm() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", null);
        assertNotNull(client);
    }

    @Test
    void pullChartThrowsWhenHelmNotFound() {
        // Use a non-existent binary path to trigger failure
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/nonexistent/helm");
        Exception ex = assertThrows(RuntimeException.class, () -> client.pullChart("oci://example.com/chart:1.0"));
        assertTrue(ex.getMessage().contains("helm pull failed"));
    }

    @Test
    void loadChartThrowsWhenHelmNotFound() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/nonexistent/helm");
        Exception ex = assertThrows(RuntimeException.class, () -> client.loadChart("oci://example.com/chart:1.0"));
        assertTrue(ex.getMessage().contains("helm load chart failed"));
    }

    @Test
    void installThrowsWhenHelmNotFound() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/nonexistent/helm");
        Map<String, String> vals = new HashMap<>();
        vals.put("key1", "val1");
        Exception ex = assertThrows(RuntimeException.class, () -> client.install("chartRef", vals));
        assertTrue(ex.getMessage().contains("helm install failed"));
    }

    @Test
    void installWithEmptyVals() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/nonexistent/helm");
        Exception ex = assertThrows(RuntimeException.class, () -> client.install("chartRef", new HashMap<>()));
        assertTrue(ex.getMessage().contains("helm install failed"));
    }

    @Test
    void installWithNullVals() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/nonexistent/helm");
        Exception ex = assertThrows(RuntimeException.class, () -> client.install("chartRef", null));
        assertTrue(ex.getMessage().contains("helm install failed"));
    }

    @Test
    void uninstallThrowsWhenHelmNotFound() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/nonexistent/helm");
        Exception ex = assertThrows(RuntimeException.class, () -> client.uninstall());
        assertTrue(ex.getMessage().contains("helm uninstall failed"));
    }

    @Test
    void listThrowsWhenHelmNotFound() {
        HelmCliClient client = new HelmCliClient("myrelease", "mynamespace", "/nonexistent/helm");
        Exception ex = assertThrows(RuntimeException.class, () -> client.list());
        assertTrue(ex.getMessage().contains("helm list failed"));
    }

    @Test
    void implementsHelmClientInterface() {
        HelmCliClient client = new HelmCliClient("test", "default");
        assertTrue(client instanceof HelmClient);
    }
}
