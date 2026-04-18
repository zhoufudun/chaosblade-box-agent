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

/**
 * Agent version information.
 * Mirrors the Go version/version.go variables.
 */
public class VersionInfo {

    private String agentVersion;
    private String env;
    private String buildTime;

    public VersionInfo() {
        this.agentVersion = "unknown";
        this.env = "unknown";
        this.buildTime = "unknown";
    }

    public VersionInfo(String agentVersion, String env, String buildTime) {
        this.agentVersion = agentVersion;
        this.env = env;
        this.buildTime = buildTime;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "agentVersion='" + agentVersion + '\'' +
                ", env='" + env + '\'' +
                ", buildTime='" + buildTime + '\'' +
                '}';
    }
}
