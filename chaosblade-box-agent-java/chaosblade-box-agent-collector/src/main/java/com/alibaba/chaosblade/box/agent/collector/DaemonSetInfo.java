package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DaemonSetInfo extends CommonInfo {
    private String namespace;
    private Integer currentNumberScheduled;
    private Integer desiredNumberScheduled;
    private Integer numberAvailable;
    private Integer numberMisscheduled;
    private Integer numberReady;
    private Long observedGeneration;
    private Integer updatedNumberScheduled;
    private String updateStrategy;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public Integer getCurrentNumberScheduled() { return currentNumberScheduled; }
    public void setCurrentNumberScheduled(Integer v) { this.currentNumberScheduled = v; }
    public Integer getDesiredNumberScheduled() { return desiredNumberScheduled; }
    public void setDesiredNumberScheduled(Integer v) { this.desiredNumberScheduled = v; }
    public Integer getNumberAvailable() { return numberAvailable; }
    public void setNumberAvailable(Integer v) { this.numberAvailable = v; }
    public Integer getNumberMisscheduled() { return numberMisscheduled; }
    public void setNumberMisscheduled(Integer v) { this.numberMisscheduled = v; }
    public Integer getNumberReady() { return numberReady; }
    public void setNumberReady(Integer v) { this.numberReady = v; }
    public Long getObservedGeneration() { return observedGeneration; }
    public void setObservedGeneration(Long v) { this.observedGeneration = v; }
    public Integer getUpdatedNumberScheduled() { return updatedNumberScheduled; }
    public void setUpdatedNumberScheduled(Integer v) { this.updatedNumberScheduled = v; }
    public String getUpdateStrategy() { return updateStrategy; }
    public void setUpdateStrategy(String v) { this.updateStrategy = v; }
}
