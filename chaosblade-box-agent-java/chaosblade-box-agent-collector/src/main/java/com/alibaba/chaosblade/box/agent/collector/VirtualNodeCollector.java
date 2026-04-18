package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Collects Node + Pod data as VirtualNode resources.
 * Mirrors Go's collector/kubernetes/virtualnode.go.
 */
public class VirtualNodeCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(VirtualNodeCollector.class);

    public VirtualNodeCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.VIRTUAL_NODE_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_VIRTUAL_NODE));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[VIRTUALNODE] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            NodeList nodeList = k8sChannel.getClient().nodes().list();
            List<VirtualNodeInfo> infos = new ArrayList<>();
            for (Node node : nodeList.getItems()) {
                VirtualNodeInfo info = new VirtualNodeInfo();
                info.setUid(node.getMetadata().getUid());
                info.setName(node.getMetadata().getName());
                info.setLabels(node.getMetadata().getLabels());
                info.setExist(true);
                if (node.getMetadata().getCreationTimestamp() != null) {
                    info.setCreatedTime(node.getMetadata().getCreationTimestamp());
                }
                info.setRole(NodeCollector.findNodeRoles(node));
                // Capacity
                if (node.getStatus() != null && node.getStatus().getAllocatable() != null) {
                    VirtualNodeInfo.NodeCapacity cap = new VirtualNodeInfo.NodeCapacity();
                    Quantity cpu = node.getStatus().getAllocatable().get("cpu");
                    Quantity mem = node.getStatus().getAllocatable().get("memory");
                    if (cpu != null) cap.setCpu(cpu.toString());
                    if (mem != null) cap.setMemory(mem.toString());
                    info.setCapacity(cap);
                }
                // Pods on this node
                List<PodInfo> pods = getPodsOnNode(node.getMetadata().getName());
                info.setPods(pods);

                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    VirtualNodeInfo minimal = new VirtualNodeInfo();
                    minimal.setUid(info.getUid());
                    minimal.setExist(true);
                    ResourceIdentifier ri = identifiers.get(info.getUid());
                    if (ri != null) minimal.setCid(ri.getCid());
                    infos.add(minimal);
                }
            }
            reportK8sMetric("", true, infos, infos.size());
            reportNotExistResource();
        } catch (Exception e) {
            logger.warn("[VIRTUALNODE] collect failed: {}", e.getMessage());
        }
    }

    private List<PodInfo> getPodsOnNode(String nodeName) {
        List<PodInfo> pods = new ArrayList<>();
        try {
            PodList podList = k8sChannel.getClient().pods().inAnyNamespace()
                    .withField("spec.nodeName", nodeName).list();
            for (Pod pod : podList.getItems()) {
                PodInfo info = new PodInfo();
                info.setUid(pod.getMetadata().getUid());
                info.setName(pod.getMetadata().getName());
                info.setNamespace(pod.getMetadata().getNamespace());
                info.setLabels(pod.getMetadata().getLabels());
                info.setExist(true);
                if (pod.getMetadata().getCreationTimestamp() != null) {
                    info.setCreatedTime(pod.getMetadata().getCreationTimestamp());
                }
                if (pod.getStatus() != null) {
                    info.setIp(pod.getStatus().getPodIP());
                    info.setState(PodCollector.getPodState(pod));
                    info.setRestartCount(PodCollector.getPodRestartCount(pod));
                }
                pods.add(info);
            }
        } catch (Exception e) {
            logger.warn("[VIRTUALNODE] get pods on node {} failed: {}", nodeName, e.getMessage());
        }
        return pods;
    }
}
