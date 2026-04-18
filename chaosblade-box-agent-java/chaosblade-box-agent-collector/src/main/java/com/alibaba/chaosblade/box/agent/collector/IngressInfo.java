package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IngressInfo extends CommonInfo {
    private String namespace;
    private String address;
    private Map<String, String> annotations;
    private List<IngressRule> rules;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Map<String, String> getAnnotations() { return annotations; }
    public void setAnnotations(Map<String, String> annotations) { this.annotations = annotations; }
    public List<IngressRule> getRules() { return rules; }
    public void setRules(List<IngressRule> rules) { this.rules = rules; }

    /** Ingress rule with host and HTTP paths. */
    public static class IngressRule {
        private String host;
        private List<HttpPath> paths;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public List<HttpPath> getPaths() { return paths; }
        public void setPaths(List<HttpPath> paths) { this.paths = paths; }
    }

    /** HTTP path with backend service reference. */
    public static class HttpPath {
        private String path;
        private String serviceName;
        private String servicePort;
        private String serviceUid;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getServicePort() { return servicePort; }
        public void setServicePort(String servicePort) { this.servicePort = servicePort; }
        public String getServiceUid() { return serviceUid; }
        public void setServiceUid(String serviceUid) { this.serviceUid = serviceUid; }
    }
}
