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

package com.alibaba.chaosblade.box.agent.monitor;

/**
 * Monitor action result from health check.
 * Mirrors Go's monitor/monitor.go monitorAction.
 */
public class MonitorAction {

    private boolean needStop;
    private boolean needStart;
    private boolean needExit;
    private String reason;

    public MonitorAction() {
        recover();
    }

    public void recover() {
        this.needStop = false;
        this.needStart = false;
        this.needExit = false;
        this.reason = "";
    }

    public boolean isNeedStop() {
        return needStop;
    }

    public void setNeedStop(boolean needStop) {
        this.needStop = needStop;
    }

    public boolean isNeedStart() {
        return needStart;
    }

    public void setNeedStart(boolean needStart) {
        this.needStart = needStart;
    }

    public boolean isNeedExit() {
        return needExit;
    }

    public void setNeedExit(boolean needExit) {
        this.needExit = needExit;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
