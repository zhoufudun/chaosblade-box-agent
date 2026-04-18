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

package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Common info fields shared by all K8s resource DTOs.
 * Mirrors Go's collector/kubernetes/kubernetes.go CommonInfo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonInfo {

    private String uid;
    private String name;
    private String createdTime;
    private Map<String, String> labels;
    private boolean exist;
    private String cid;

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public Map<String, String> getLabels() { return labels; }
    public void setLabels(Map<String, String> labels) { this.labels = labels; }

    public boolean isExist() { return exist; }
    public void setExist(boolean exist) { this.exist = exist; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }
}
