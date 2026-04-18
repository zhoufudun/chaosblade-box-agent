package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ReplicaSetCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(ReplicaSetCollector.class);

    public ReplicaSetCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.REPLICASET_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_REPLICASET));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[REPLICASET] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            ReplicaSetList list = k8sChannel.getClient().apps().replicaSets().inAnyNamespace().list();
            List<ReplicaSetInfo> infos = new ArrayList<>();
            for (ReplicaSet rs : list.getItems()) {
                ReplicaSetInfo info = new ReplicaSetInfo();
                info.setUid(rs.getMetadata().getUid());
                info.setName(rs.getMetadata().getName());
                info.setNamespace(rs.getMetadata().getNamespace());
                info.setLabels(rs.getMetadata().getLabels());
                info.setExist(true);
                if (rs.getMetadata().getCreationTimestamp() != null) {
                    info.setCreatedTime(rs.getMetadata().getCreationTimestamp());
                }
                if (rs.getStatus() != null) {
                    info.setAvailableReplicas(rs.getStatus().getAvailableReplicas());
                    info.setReplicas(rs.getStatus().getReplicas());
                    info.setReadyReplicas(rs.getStatus().getReadyReplicas());
                    info.setObservedGeneration(rs.getStatus().getObservedGeneration());
                }
                // Get deployment UID from owner references
                if (rs.getMetadata().getOwnerReferences() != null) {
                    for (OwnerReference ref : rs.getMetadata().getOwnerReferences()) {
                        if ("Deployment".equals(ref.getKind())) {
                            info.setDeploymentUid(ref.getUid());
                        }
                    }
                }
                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    ReplicaSetInfo minimal = new ReplicaSetInfo();
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
            logger.warn("[REPLICASET] collect failed: {}", e.getMessage());
        }
    }
}
