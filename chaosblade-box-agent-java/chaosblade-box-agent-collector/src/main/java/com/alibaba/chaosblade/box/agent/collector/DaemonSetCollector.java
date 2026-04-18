package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DaemonSetCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(DaemonSetCollector.class);

    public DaemonSetCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.DAEMONSET_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_DAEMONSET));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[DAEMONSET] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            DaemonSetList list = k8sChannel.getClient().apps().daemonSets().inAnyNamespace().list();
            List<DaemonSetInfo> infos = new ArrayList<>();
            for (DaemonSet ds : list.getItems()) {
                DaemonSetInfo info = new DaemonSetInfo();
                info.setUid(ds.getMetadata().getUid());
                info.setName(ds.getMetadata().getName());
                info.setNamespace(ds.getMetadata().getNamespace());
                info.setLabels(ds.getMetadata().getLabels());
                info.setExist(true);
                if (ds.getMetadata().getCreationTimestamp() != null) {
                    info.setCreatedTime(ds.getMetadata().getCreationTimestamp());
                }
                if (ds.getStatus() != null) {
                    info.setCurrentNumberScheduled(ds.getStatus().getCurrentNumberScheduled());
                    info.setDesiredNumberScheduled(ds.getStatus().getDesiredNumberScheduled());
                    info.setNumberAvailable(ds.getStatus().getNumberAvailable());
                    info.setNumberMisscheduled(ds.getStatus().getNumberMisscheduled());
                    info.setNumberReady(ds.getStatus().getNumberReady());
                    info.setUpdatedNumberScheduled(ds.getStatus().getUpdatedNumberScheduled());
                    info.setObservedGeneration(ds.getStatus().getObservedGeneration());
                }
                if (ds.getSpec() != null && ds.getSpec().getUpdateStrategy() != null) {
                    info.setUpdateStrategy(ds.getSpec().getUpdateStrategy().getType());
                }
                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    DaemonSetInfo minimal = new DaemonSetInfo();
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
            logger.warn("[DAEMONSET] collect failed: {}", e.getMessage());
        }
    }
}
