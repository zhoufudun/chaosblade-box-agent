package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentInfo extends CommonInfo {
    private String namespace;
    private Integer availableReplicas;
    private Integer replicas;
    private Long observedGeneration;
    private Integer readyReplicas;
    private Integer updatedReplicas;
    private String strategy;
    private Integer unavailableReplicas;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public Integer getAvailableReplicas() { return availableReplicas; }
    public void setAvailableReplicas(Integer availableReplicas) { this.availableReplicas = availableReplicas; }
    public Integer getReplicas() { return replicas; }
    public void setReplicas(Integer replicas) { this.replicas = replicas; }
    public Long getObservedGeneration() { return observedGeneration; }
    public void setObservedGeneration(Long observedGeneration) { this.observedGeneration = observedGeneration; }
    public Integer getReadyReplicas() { return readyReplicas; }
    public void setReadyReplicas(Integer readyReplicas) { this.readyReplicas = readyReplicas; }
    public Integer getUpdatedReplicas() { return updatedReplicas; }
    public void setUpdatedReplicas(Integer updatedReplicas) { this.updatedReplicas = updatedReplicas; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public Integer getUnavailableReplicas() { return unavailableReplicas; }
    public void setUnavailableReplicas(Integer unavailableReplicas) { this.unavailableReplicas = unavailableReplicas; }
}
