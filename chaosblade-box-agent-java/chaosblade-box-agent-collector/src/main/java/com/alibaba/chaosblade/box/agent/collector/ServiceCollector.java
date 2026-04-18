package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects Service resources from K8s cluster.
 * Mirrors Go's collector/kubernetes/service.go ServiceCollector.
 */
public class ServiceCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCollector.class);

    public ServiceCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.SERVICE_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_SERVICE));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[SERVICE] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            ServiceList serviceList = k8sChannel.getClient().services().inAnyNamespace().list();
            List<ServiceInfo> infos = new ArrayList<>();
            for (Service svc : serviceList.getItems()) {
                ServiceInfo info = buildServiceInfo(svc);
                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    ServiceInfo minimal = new ServiceInfo();
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
            logger.warn("[SERVICE] collect failed: {}", e.getMessage());
        }
    }

    private ServiceInfo buildServiceInfo(Service svc) {
        ServiceInfo info = new ServiceInfo();
        info.setUid(svc.getMetadata().getUid());
        info.setName(svc.getMetadata().getName());
        info.setNamespace(svc.getMetadata().getNamespace());
        info.setLabels(svc.getMetadata().getLabels());
        info.setExist(true);
        if (svc.getMetadata().getCreationTimestamp() != null) {
            info.setCreatedTime(svc.getMetadata().getCreationTimestamp());
        }
        if (svc.getSpec() != null) {
            info.setClusterIp(svc.getSpec().getClusterIP());
            info.setType(svc.getSpec().getType());
            info.setSelector(svc.getSpec().getSelector());
            info.setPorts(servicePortsToString(svc.getSpec().getPorts()));
        }
        return info;
    }

    private List<String> servicePortsToString(List<ServicePort> ports) {
        List<String> result = new ArrayList<>();
        if (ports == null) return result;
        for (ServicePort port : ports) {
            int nodePort = port.getNodePort() != null ? port.getNodePort() : 0;
            result.add(String.format("%d->%d/%s/%s",
                    nodePort, port.getPort(), port.getProtocol(),
                    port.getName() != null ? port.getName() : ""));
        }
        return result;
    }
}
