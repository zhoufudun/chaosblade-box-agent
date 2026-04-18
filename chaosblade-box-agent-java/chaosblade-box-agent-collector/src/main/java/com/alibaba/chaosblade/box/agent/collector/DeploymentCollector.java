package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DeploymentCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentCollector.class);

    public DeploymentCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.DEPLOYMENT_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_DEPLOYMENT));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[DEPLOYMENT] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            DeploymentList list = k8sChannel.getClient().apps().deployments().inAnyNamespace().list();
            List<DeploymentInfo> infos = new ArrayList<>();
            for (Deployment d : list.getItems()) {
                DeploymentInfo info = new DeploymentInfo();
                info.setUid(d.getMetadata().getUid());
                info.setName(d.getMetadata().getName());
                info.setNamespace(d.getMetadata().getNamespace());
                info.setLabels(d.getMetadata().getLabels());
                info.setExist(true);
                if (d.getMetadata().getCreationTimestamp() != null) {
                    info.setCreatedTime(d.getMetadata().getCreationTimestamp());
                }
                if (d.getSpec() != null) {
                    info.setReplicas(d.getSpec().getReplicas());
                    if (d.getSpec().getStrategy() != null) {
                        info.setStrategy(d.getSpec().getStrategy().getType());
                    }
                }
                if (d.getStatus() != null) {
                    info.setAvailableReplicas(d.getStatus().getAvailableReplicas());
                    info.setReadyReplicas(d.getStatus().getReadyReplicas());
                    info.setUpdatedReplicas(d.getStatus().getUpdatedReplicas());
                    info.setUnavailableReplicas(d.getStatus().getUnavailableReplicas());
                    info.setObservedGeneration(d.getStatus().getObservedGeneration());
                }
                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    DeploymentInfo minimal = new DeploymentInfo();
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
            logger.warn("[DEPLOYMENT] collect failed: {}", e.getMessage());
        }
    }
}
