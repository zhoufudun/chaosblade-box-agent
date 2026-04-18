package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceInfo extends CommonInfo {
    private String namespace;
    private String clusterIp;
    private String externalIp;
    private List<String> ports;
    private String type;
    private Map<String, String> selector;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getClusterIp() { return clusterIp; }
    public void setClusterIp(String clusterIp) { this.clusterIp = clusterIp; }
    public String getExternalIp() { return externalIp; }
    public void setExternalIp(String externalIp) { this.externalIp = externalIp; }
    public List<String> getPorts() { return ports; }
    public void setPorts(List<String> ports) { this.ports = ports; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, String> getSelector() { return selector; }
    public void setSelector(Map<String, String> selector) { this.selector = selector; }
}
