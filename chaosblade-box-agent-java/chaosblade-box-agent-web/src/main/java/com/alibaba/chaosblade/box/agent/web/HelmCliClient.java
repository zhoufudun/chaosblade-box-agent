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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CLI-based implementation of {@link HelmClient}.
 * Executes helm commands via process invocation since there is no Helm Java SDK.
 * Mirrors Go's pkg/helm3/helm.go Helm struct.
 */
public class HelmCliClient implements HelmClient {

    private static final Logger logger = LoggerFactory.getLogger(HelmCliClient.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    private final String helmName;
    private final String helmNamespace;
    private final String helmBinaryPath;

    /**
     * Create a new HelmCliClient.
     *
     * @param helmName      the release name for install/uninstall
     * @param helmNamespace the namespace for helm operations
     */
    public HelmCliClient(String helmName, String helmNamespace) {
        this(helmName, helmNamespace, "helm");
    }

    /**
     * Create a new HelmCliClient with a custom helm binary path.
     *
     * @param helmName       the release name for install/uninstall
     * @param helmNamespace  the namespace for helm operations
     * @param helmBinaryPath path to the helm binary (default: "helm")
     */
    public HelmCliClient(String helmName, String helmNamespace, String helmBinaryPath) {
        this.helmName = helmName;
        this.helmNamespace = helmNamespace;
        this.helmBinaryPath = helmBinaryPath != null ? helmBinaryPath : "helm";
    }

    @Override
    public void pullChart(String chartUrl) throws Exception {
        logger.info("[helm] pulling chart: {}", chartUrl);
        String args = "pull " + chartUrl + " --untar";
        ExecResult result = execHelm(args);
        if (!result.isSuccess()) {
            throw new RuntimeException("helm pull failed: " + result.getErrorMsg());
        }
        logger.info("[helm] pull chart success: {}", chartUrl);
    }

    @Override
    public Object loadChart(String chartUrl) throws Exception {
        logger.info("[helm] loading chart: {}", chartUrl);
        // In CLI mode, loadChart validates the chart is accessible and returns
        // the chartUrl as the chart reference for install.
        // helm show chart <chartUrl> verifies the chart exists.
        String args = "show chart " + chartUrl;
        ExecResult result = execHelm(args);
        if (!result.isSuccess()) {
            throw new RuntimeException("helm load chart failed: " + result.getErrorMsg());
        }
        logger.info("[helm] load chart success: {}", chartUrl);
        return chartUrl;
    }

    @Override
    public void install(Object chart, Map<String, String> vals) throws Exception {
        String chartRef = String.valueOf(chart);
        logger.info("[helm] installing chart: {}, release: {}, namespace: {}", chartRef, helmName, helmNamespace);

        StringBuilder args = new StringBuilder();
        args.append("install ").append(helmName).append(" ").append(chartRef);
        args.append(" --namespace ").append(helmNamespace);

        // Append --set values
        if (vals != null && !vals.isEmpty()) {
            List<String> setPairs = new ArrayList<>();
            for (Map.Entry<String, String> entry : vals.entrySet()) {
                setPairs.add(entry.getKey() + "=" + entry.getValue());
            }
            args.append(" --set ");
            args.append(join(setPairs, ","));
        }

        ExecResult result = execHelm(args.toString());
        if (!result.isSuccess()) {
            throw new RuntimeException("helm install failed: " + result.getErrorMsg());
        }
        logger.info("[helm] install success, release: {}", helmName);
    }

    @Override
    public void uninstall() throws Exception {
        logger.info("[helm] uninstalling release: {}, namespace: {}", helmName, helmNamespace);
        String args = "uninstall " + helmName + " --namespace " + helmNamespace;
        ExecResult result = execHelm(args);
        if (!result.isSuccess()) {
            throw new RuntimeException("helm uninstall failed: " + result.getErrorMsg());
        }
        logger.info("[helm] uninstall success, release: {}", helmName);
    }

    /**
     * List installed releases in the configured namespace.
     *
     * @return the output of helm list
     * @throws Exception if the command fails
     */
    public String list() throws Exception {
        logger.info("[helm] listing releases, namespace: {}", helmNamespace);
        String args = "list --namespace " + helmNamespace;
        ExecResult result = execHelm(args);
        if (!result.isSuccess()) {
            throw new RuntimeException("helm list failed: " + result.getErrorMsg());
        }
        return result.getOutput();
    }

    /**
     * Execute a helm command with the given arguments.
     */
    private ExecResult execHelm(String args) {
        String command = helmBinaryPath + " " + args;
        logger.info("[helm] executing: {}", command);

        try {
            ProcessBuilder pb;
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("windows")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", command);
            }
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("[helm] command timed out after {}s", DEFAULT_TIMEOUT_SECONDS);
                return new ExecResult(output.toString(), "timeout after " + DEFAULT_TIMEOUT_SECONDS + "s", false);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.warn("[helm] command exited with code: {}, output: {}", exitCode, output.toString().trim());
                return new ExecResult(output.toString(), output.toString().trim(), false);
            }

            return new ExecResult(output.toString(), "", true);
        } catch (Exception e) {
            logger.error("[helm] command execution failed", e);
            return new ExecResult("", e.getMessage(), false);
        }
    }

    /**
     * Join a list of strings with a delimiter. Java 8 compatible.
     */
    private static String join(List<String> parts, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /**
     * Result of a helm command execution.
     */
    static class ExecResult {
        private final String output;
        private final String errorMsg;
        private final boolean success;

        ExecResult(String output, String errorMsg, boolean success) {
            this.output = output;
            this.errorMsg = errorMsg;
            this.success = success;
        }

        String getOutput() { return output; }
        String getErrorMsg() { return errorMsg; }
        boolean isSuccess() { return success; }
    }
}
