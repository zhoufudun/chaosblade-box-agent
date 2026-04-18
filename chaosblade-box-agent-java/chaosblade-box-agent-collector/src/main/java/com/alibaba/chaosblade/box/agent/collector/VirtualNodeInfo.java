package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VirtualNodeInfo extends CommonInfo {
    private String role;
    private String clusterId;
    private String clusterName;
    private Map<String, String> nodeInfo;
    private NodeCapacity capacity;
    private List<PodInfo> pods;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }
    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }
    public Map<String, String> getNodeInfo() { return nodeInfo; }
    public void setNodeInfo(Map<String, String> nodeInfo) { this.nodeInfo = nodeInfo; }
    public NodeCapacity getCapacity() { return capacity; }
    public void setCapacity(NodeCapacity capacity) { this.capacity = capacity; }
    public List<PodInfo> getPods() { return pods; }
    public void setPods(List<PodInfo> pods) { this.pods = pods; }

    public static class NodeCapacity {
        private String cpu;
        private String memory;

        public String getCpu() { return cpu; }
        public void setCpu(String cpu) { this.cpu = cpu; }
        public String getMemory() { return memory; }
        public void setMemory(String memory) { this.memory = memory; }
    }
}
