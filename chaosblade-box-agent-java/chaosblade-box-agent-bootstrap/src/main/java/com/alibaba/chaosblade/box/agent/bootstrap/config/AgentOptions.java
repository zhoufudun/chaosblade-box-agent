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

package com.alibaba.chaosblade.box.agent.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Agent configuration options, mapped from application.properties or command-line arguments.
 * Mirrors the Go Options struct from pkg/options/options.go.
 */
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentOptions {

    // --- Constants ---
    public static final String PROGRAM_NAME = "CHAOS_AGENT";
    public static final String BLADE_PROGRAM_NAME = "CHAOS_BLADE";

    public static final String LOG_FILE_OUTPUT = "file";
    public static final String LOG_STD_OUTPUT = "stdout";

    public static final String AGENT_HOST_MODE = "host";
    public static final String AGENT_K8S_MODE = "k8s";
    public static final String AGENT_CS_K8S_MODE = "cs_k8s";
    public static final String AGENT_CS_SWARM_MODE = "cs_swarm";
    public static final String AGENT_K8S_HELM_MODE = "k8s_helm";
    public static final String AGENT_CS_K8S_HELM_MODE = "cs_k8s_helm";

    public static final String INSTALL_OPERATOR_LINUX = "linux";
    public static final String INSTALL_OPERATOR_WINDOWS = "windows";

    public static final String APP_INSTANCE_KEY_NAME = "appInstance";
    public static final String APP_GROUP_KEY_NAME = "appGroup";
    public static final String DEFAULT_APPLICATION_INSTANCE = "chaos-default-app";
    public static final String DEFAULT_APPLICATION_GROUP = "chaos-default-app-group";

    public static final String START_MANUAL_MODE = "manual";
    public static final String START_CONSOLE_MODE = "console";
    public static final String START_CRONTAB_MODE = "crontab";
    public static final String START_UPGRADE_MODE = "upgrade";

    // --- Log config ---
    private String logLevel = "info";
    private int logMaxFileSize = 10;
    private int logMaxFileCount = 1;
    private String logOutput = LOG_FILE_OUTPUT;

    // --- Environment ---
    private String environment = "prod";
    private String namespace = "default";
    private String license = "";
    private String mode = AGENT_HOST_MODE;
    private String installOperator = INSTALL_OPERATOR_LINUX;

    // --- Heartbeat ---
    private Duration heartbeatPeriod = Duration.ofSeconds(5);

    // --- Transport ---
    private String transportEndpoint = "";
    private Duration transportTimeout = Duration.ofSeconds(3);
    private boolean transportSecure = true;

    // --- Application ---
    private String applicationInstance = DEFAULT_APPLICATION_INSTANCE;
    private String applicationGroup = DEFAULT_APPLICATION_GROUP;
    private String startupMode = START_CONSOLE_MODE;
    private boolean restrictedVpc = false;

    // --- Kubernetes ---
    private String clusterId = "";
    private String clusterName = "";
    private boolean podMetricFlag = false;
    private boolean externalIpEnable = false;

    // --- Port ---
    private String port = "19527";

    // --- Agent IP ---
    private String localIp = "";

    // --- Download URLs ---
    private String agentBinUrl = "";
    private String agentShUrl = "";
    private String bladeTarUrl = "";
    private String litmusChartUrl = "";
    private String certUrl = "";

    // --- Runtime (set programmatically, not from config file) ---
    private volatile String pid = "";
    private volatile String ip = "";
    private volatile String hostName = "";
    private volatile String uid = "";
    private volatile String cid = "";
    private volatile String chaosbladeVersion = "";
    private volatile String litmusChaosVersion = "";
    private String version = "1.1.0";
    private boolean isVpc = false;
    private String vpcId = "";
    private String instanceId = "";

    // --- Getters and Setters ---

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public int getLogMaxFileSize() { return logMaxFileSize; }
    public void setLogMaxFileSize(int logMaxFileSize) { this.logMaxFileSize = logMaxFileSize; }

    public int getLogMaxFileCount() { return logMaxFileCount; }
    public void setLogMaxFileCount(int logMaxFileCount) { this.logMaxFileCount = logMaxFileCount; }

    public String getLogOutput() { return logOutput; }
    public void setLogOutput(String logOutput) { this.logOutput = logOutput; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getInstallOperator() { return installOperator; }
    public void setInstallOperator(String installOperator) { this.installOperator = installOperator; }

    public Duration getHeartbeatPeriod() { return heartbeatPeriod; }
    public void setHeartbeatPeriod(Duration heartbeatPeriod) { this.heartbeatPeriod = heartbeatPeriod; }

    public String getTransportEndpoint() { return transportEndpoint; }
    public void setTransportEndpoint(String transportEndpoint) { this.transportEndpoint = transportEndpoint; }

    public Duration getTransportTimeout() { return transportTimeout; }
    public void setTransportTimeout(Duration transportTimeout) { this.transportTimeout = transportTimeout; }

    public boolean isTransportSecure() { return transportSecure; }
    public void setTransportSecure(boolean transportSecure) { this.transportSecure = transportSecure; }

    public String getApplicationInstance() { return applicationInstance; }
    public void setApplicationInstance(String applicationInstance) { this.applicationInstance = applicationInstance; }

    public String getApplicationGroup() { return applicationGroup; }
    public void setApplicationGroup(String applicationGroup) { this.applicationGroup = applicationGroup; }

    public String getStartupMode() { return startupMode; }
    public void setStartupMode(String startupMode) { this.startupMode = startupMode; }

    public boolean isRestrictedVpc() { return restrictedVpc; }
    public void setRestrictedVpc(boolean restrictedVpc) { this.restrictedVpc = restrictedVpc; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public boolean isPodMetricFlag() { return podMetricFlag; }
    public void setPodMetricFlag(boolean podMetricFlag) { this.podMetricFlag = podMetricFlag; }

    public boolean isExternalIpEnable() { return externalIpEnable; }
    public void setExternalIpEnable(boolean externalIpEnable) { this.externalIpEnable = externalIpEnable; }

    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }

    public String getLocalIp() { return localIp; }
    public void setLocalIp(String localIp) { this.localIp = localIp; }

    public String getAgentBinUrl() { return agentBinUrl; }
    public void setAgentBinUrl(String agentBinUrl) { this.agentBinUrl = agentBinUrl; }

    public String getAgentShUrl() { return agentShUrl; }
    public void setAgentShUrl(String agentShUrl) { this.agentShUrl = agentShUrl; }

    public String getBladeTarUrl() { return bladeTarUrl; }
    public void setBladeTarUrl(String bladeTarUrl) { this.bladeTarUrl = bladeTarUrl; }

    public String getLitmusChartUrl() { return litmusChartUrl; }
    public void setLitmusChartUrl(String litmusChartUrl) { this.litmusChartUrl = litmusChartUrl; }

    public String getCertUrl() { return certUrl; }
    public void setCertUrl(String certUrl) { this.certUrl = certUrl; }

    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public String getChaosbladeVersion() { return chaosbladeVersion; }
    public void setChaosbladeVersion(String chaosbladeVersion) { this.chaosbladeVersion = chaosbladeVersion; }

    public String getLitmusChaosVersion() { return litmusChaosVersion; }
    public void setLitmusChaosVersion(String litmusChaosVersion) { this.litmusChaosVersion = litmusChaosVersion; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public boolean isVpc() { return isVpc; }
    public void setVpc(boolean vpc) { isVpc = vpc; }

    public String getVpcId() { return vpcId; }
    public void setVpcId(String vpcId) { this.vpcId = vpcId; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    // --- Convenience methods ---

    /**
     * Check if agent is running in any Kubernetes mode.
     */
    public boolean isK8sMode() {
        return AGENT_K8S_MODE.equals(mode)
                || AGENT_CS_K8S_MODE.equals(mode)
                || AGENT_K8S_HELM_MODE.equals(mode)
                || AGENT_CS_K8S_HELM_MODE.equals(mode);
    }

    /**
     * Check if agent is running in host mode.
     */
    public boolean isHostMode() {
        return AGENT_HOST_MODE.equals(mode);
    }

    /**
     * Set cluster ID only if not already set.
     */
    public void setClusterIdIfNotPresent(String clusterId) {
        if (this.clusterId == null || this.clusterId.isEmpty()) {
            this.clusterId = clusterId;
        }
    }
}
