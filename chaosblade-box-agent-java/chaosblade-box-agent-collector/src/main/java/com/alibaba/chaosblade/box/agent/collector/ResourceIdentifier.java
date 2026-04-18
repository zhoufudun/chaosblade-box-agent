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

/**
 * Tracks a K8s resource's identity, CID from server, MD5 for incremental updates,
 * and current existence status.
 * Mirrors Go's collector/kubernetes/kubernetes.go ResourceIdentifier.
 */
public class ResourceIdentifier {

    private String uid;
    private String cid;
    private String md5;
    /** Whether the resource currently exists in the cluster. */
    private boolean curr;
    /** Cached name, not sent to backend. */
    private String name;

    public ResourceIdentifier() {
    }

    public ResourceIdentifier(String uid, String md5, boolean curr, String name) {
        this.uid = uid;
        this.md5 = md5;
        this.curr = curr;
        this.name = name;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public String getMd5() { return md5; }
    public void setMd5(String md5) { this.md5 = md5; }

    public boolean isCurr() { return curr; }
    public void setCurr(boolean curr) { this.curr = curr; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
