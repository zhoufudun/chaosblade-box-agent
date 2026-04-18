package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PodInfo extends CommonInfo {
    private String namespace;
    private String ip;
    private Integer restartCount;
    private String state;
    private String daemonsetUid;
    private String daemonsetCid;
    private String serviceUid;
    private String serviceCid;
    private String deploymentUid;
    private String deploymentCid;
    private String replicasetUid;
    private String replicasetCid;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Integer getRestartCount() { return restartCount; }
    public void setRestartCount(Integer restartCount) { this.restartCount = restartCount; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getDaemonsetUid() { return daemonsetUid; }
    public void setDaemonsetUid(String daemonsetUid) { this.daemonsetUid = daemonsetUid; }
    public String getDaemonsetCid() { return daemonsetCid; }
    public void setDaemonsetCid(String daemonsetCid) { this.daemonsetCid = daemonsetCid; }
    public String getServiceUid() { return serviceUid; }
    public void setServiceUid(String serviceUid) { this.serviceUid = serviceUid; }
    public String getServiceCid() { return serviceCid; }
    public void setServiceCid(String serviceCid) { this.serviceCid = serviceCid; }
    public String getDeploymentUid() { return deploymentUid; }
    public void setDeploymentUid(String deploymentUid) { this.deploymentUid = deploymentUid; }
    public String getDeploymentCid() { return deploymentCid; }
    public void setDeploymentCid(String deploymentCid) { this.deploymentCid = deploymentCid; }
    public String getReplicasetUid() { return replicasetUid; }
    public void setReplicasetUid(String replicasetUid) { this.replicasetUid = replicasetUid; }
    public String getReplicasetCid() { return replicasetCid; }
    public void setReplicasetCid(String replicasetCid) { this.replicasetCid = replicasetCid; }

    /** Add link from selector matching. */
    public void addLink(String resource, String uid) {
        switch (resource) {
            case K8sChannel.SERVICE_RESOURCE: setServiceUid(uid); break;
            case K8sChannel.DEPLOYMENT_RESOURCE: setDeploymentUid(uid); break;
            case K8sChannel.REPLICASET_RESOURCE: setReplicasetUid(uid); break;
            case K8sChannel.DAEMONSET_RESOURCE: setDaemonsetUid(uid); break;
            default: break;
        }
    }
}
