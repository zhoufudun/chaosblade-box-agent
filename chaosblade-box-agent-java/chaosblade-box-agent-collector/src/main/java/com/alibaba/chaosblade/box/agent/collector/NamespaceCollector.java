package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NamespaceCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(NamespaceCollector.class);

    public NamespaceCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.NAMESPACE_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_NAMESPACE));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[NAMESPACE] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            NamespaceList list = k8sChannel.getClient().namespaces().list();
            List<NamespaceInfo> infos = new ArrayList<>();
            for (Namespace ns : list.getItems()) {
                NamespaceInfo info = new NamespaceInfo();
                info.setUid(ns.getMetadata().getUid());
                info.setName(ns.getMetadata().getName());
                info.setLabels(ns.getMetadata().getLabels());
                info.setExist(true);
                if (ns.getMetadata().getCreationTimestamp() != null) {
                    info.setCreatedTime(ns.getMetadata().getCreationTimestamp());
                }
                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    NamespaceInfo minimal = new NamespaceInfo();
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
            logger.warn("[NAMESPACE] collect failed: {}", e.getMessage());
        }
    }
}
