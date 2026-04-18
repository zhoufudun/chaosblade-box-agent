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

package com.alibaba.chaosblade.box.agent.pkg;

/**
 * Global agent context holder, similar to Go's options.Opts.
 * Provides agent identity info needed by Request.newRequest() across modules.
 */
public final class AgentContext {

    private static volatile String pid = "";
    private static volatile String uid = "";
    private static volatile String cid = "";
    private static volatile String programName = "CHAOS_AGENT";
    private static volatile String version = "1.1.0";
    private static volatile String chaosbladeVersion = "";
    private static volatile String port = "19527";

    private AgentContext() {}

    public static void init(String pid, String uid, String cid,
                            String programName, String version,
                            String chaosbladeVersion, String port) {
        AgentContext.pid = pid;
        AgentContext.uid = uid;
        AgentContext.cid = cid;
        AgentContext.programName = programName;
        AgentContext.version = version;
        AgentContext.chaosbladeVersion = chaosbladeVersion;
        AgentContext.port = port;
    }

    public static void setCid(String cid) { AgentContext.cid = cid; }
    public static void setUid(String uid) { AgentContext.uid = uid; }

    public static String getPid() { return pid; }
    public static String getUid() { return uid; }
    public static String getCid() { return cid; }
    public static String getProgramName() { return programName; }
    public static String getVersion() { return version; }
    public static String getChaosbladeVersion() { return chaosbladeVersion; }
    public static String getPort() { return port; }
}
