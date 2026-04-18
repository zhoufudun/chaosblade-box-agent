package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import com.alibaba.chaosblade.box.agent.transport.Uri;
import io.fabric8.kubernetes.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects Pod resources from K8s cluster.
 * Mirrors Go's collector/kubernetes/pod.go PodCollector.
 */
public class PodCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(PodCollector.class);

    public PodCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.POD_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_POD));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[POD] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            PodList podList = k8sChannel.getClient().pods().inAnyNamespace().list();
            List<PodInfo> infos = new ArrayList<>();
            for (Pod pod : podList.getItems()) {
                PodInfo info = buildPodInfo(pod);
                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    // Unchanged - report minimal
                    PodInfo minimal = new PodInfo();
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
            logger.warn("[POD] collect failed: {}", e.getMessage());
        }
    }

    private PodInfo buildPodInfo(Pod pod) {
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
            info.setState(getPodState(pod));
            info.setRestartCount(getPodRestartCount(pod));
        }
        // Owner references
        if (pod.getMetadata().getOwnerReferences() != null) {
            for (OwnerReference ref : pod.getMetadata().getOwnerReferences()) {
                if ("ReplicaSet".equals(ref.getKind())) {
                    info.setReplicasetUid(ref.getUid());
                } else if ("DaemonSet".equals(ref.getKind())) {
                    info.setDaemonsetUid(ref.getUid());
                }
            }
        }
        return info;
    }

    static int getPodRestartCount(Pod pod) {
        int count = 0;
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                count += cs.getRestartCount();
            }
        }
        return count;
    }

    static String getPodState(Pod pod) {
        String reason = pod.getStatus().getPhase();
        if (pod.getStatus().getReason() != null && !pod.getStatus().getReason().isEmpty()) {
            reason = pod.getStatus().getReason();
        }
        // Simplified state detection matching Go logic
        if (pod.getMetadata().getDeletionTimestamp() != null) {
            if ("NodeLost".equals(pod.getStatus().getReason())) {
                return "Unknown";
            }
            return "Terminating";
        }
        if (pod.getStatus().getContainerStatuses() != null) {
            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                if (cs.getState() != null && cs.getState().getWaiting() != null
                        && cs.getState().getWaiting().getReason() != null) {
                    reason = cs.getState().getWaiting().getReason();
                }
            }
        }
        return reason;
    }
}
