package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(NodeCollector.class);
    private static final String LABEL_NODE_ROLE_PREFIX = "node-role.kubernetes.io/";
    private static final String NODE_LABEL_ROLE = "kubernetes.io/role";

    private final String clusterId;

    public NodeCollector(K8sChannel k8sChannel, TransportClient transportClient, String clusterId) {
        super(K8sChannel.NODE_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_NODE));
        this.clusterId = clusterId;
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[NODE] k8s client not available");
            return;
        }
        try {
            NodeList nodeList = k8sChannel.getClient().nodes().list();
            List<NodeInfo> infos = new ArrayList<>();
            for (Node node : nodeList.getItems()) {
                NodeInfo info = new NodeInfo();
                info.setUid(node.getMetadata().getUid());
                info.setName(node.getMetadata().getName());
                info.setClusterId(clusterId);
                info.setRole(findNodeRoles(node));
                infos.add(info);
                break; // Go code only reports first node
            }
            reportK8sMetric("", true, infos, infos.size());
        } catch (Exception e) {
            logger.warn("[NODE] collect failed: {}", e.getMessage());
        }
    }

    static String findNodeRoles(Node node) {
        List<String> roles = new ArrayList<>();
        Map<String, String> labels = node.getMetadata().getLabels();
        if (labels != null) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                if (entry.getKey().startsWith(LABEL_NODE_ROLE_PREFIX)) {
                    String role = entry.getKey().substring(LABEL_NODE_ROLE_PREFIX.length());
                    if (!role.isEmpty()) roles.add(role);
                } else if (NODE_LABEL_ROLE.equals(entry.getKey()) && !entry.getValue().isEmpty()) {
                    roles.add(entry.getValue());
                }
            }
        }
        return roles.isEmpty() ? "<none>" : String.join(",", roles);
    }
}
