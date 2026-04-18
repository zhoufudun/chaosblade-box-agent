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

import java.time.Duration;

/**
 * Configuration POJO for the conn module.
 * Avoids circular dependency on bootstrap's AgentOptions by holding
 * all needed config values locally.
 */
public class ConnConfig {

    private String ip = "";
    private String pid = "";
    private String uid = "";
    private String instanceId = "";
    private String namespace = "default";
    private String license = "";
    private String startupMode = "console";
    private String version = "1.1.0";
    private String mode = "host";
    private String installOperator = "linux";
    private String chaosbladeVersion = "";
    private String clusterId = "";
    private String clusterName = "";
    private String applicationInstance = "chaos-default-app";
    private String applicationGroup = "chaos-default-app-group";
    private boolean restrictedVpc = false;
    private String vpcId = "";
    private boolean externalIpEnable = false;
    private String port = "19527";

    private volatile String cid = "";

    private Duration heartbeatPeriod = Duration.ofSeconds(5);
    private Duration metricPeriod = Duration.ofSeconds(10);

    // --- Getters and Setters ---

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }

    public String getStartupMode() { return startupMode; }
    public void setStartupMode(String startupMode) { this.startupMode = startupMode; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getInstallOperator() { return installOperator; }
    public void setInstallOperator(String installOperator) { this.installOperator = installOperator; }

    public String getChaosbladeVersion() { return chaosbladeVersion; }
    public void setChaosbladeVersion(String chaosbladeVersion) { this.chaosbladeVersion = chaosbladeVersion; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getApplicationInstance() { return applicationInstance; }
    public void setApplicationInstance(String applicationInstance) { this.applicationInstance = applicationInstance; }

    public String getApplicationGroup() { return applicationGroup; }
    public void setApplicationGroup(String applicationGroup) { this.applicationGroup = applicationGroup; }

    public boolean isRestrictedVpc() { return restrictedVpc; }
    public void setRestrictedVpc(boolean restrictedVpc) { this.restrictedVpc = restrictedVpc; }

    public String getVpcId() { return vpcId; }
    public void setVpcId(String vpcId) { this.vpcId = vpcId; }

    public boolean isExternalIpEnable() { return externalIpEnable; }
    public void setExternalIpEnable(boolean externalIpEnable) { this.externalIpEnable = externalIpEnable; }

    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public Duration getHeartbeatPeriod() { return heartbeatPeriod; }
    public void setHeartbeatPeriod(Duration heartbeatPeriod) { this.heartbeatPeriod = heartbeatPeriod; }

    public Duration getMetricPeriod() { return metricPeriod; }
    public void setMetricPeriod(Duration metricPeriod) { this.metricPeriod = metricPeriod; }
}
