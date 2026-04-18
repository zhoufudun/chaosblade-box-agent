package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplicaSetInfo extends CommonInfo {
    private String namespace;
    private Integer availableReplicas;
    private Integer replicas;
    private Long observedGeneration;
    private Integer readyReplicas;
    private String deploymentUid;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public Integer getAvailableReplicas() { return availableReplicas; }
    public void setAvailableReplicas(Integer v) { this.availableReplicas = v; }
    public Integer getReplicas() { return replicas; }
    public void setReplicas(Integer v) { this.replicas = v; }
    public Long getObservedGeneration() { return observedGeneration; }
    public void setObservedGeneration(Long v) { this.observedGeneration = v; }
    public Integer getReadyReplicas() { return readyReplicas; }
    public void setReadyReplicas(Integer v) { this.readyReplicas = v; }
    public String getDeploymentUid() { return deploymentUid; }
    public void setDeploymentUid(String v) { this.deploymentUid = v; }
}
