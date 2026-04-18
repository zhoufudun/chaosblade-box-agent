package com.alibaba.chaosblade.box.agent.collector;

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressList;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class IngressCollector extends K8sBaseCollector {

    private static final Logger logger = LoggerFactory.getLogger(IngressCollector.class);

    public IngressCollector(K8sChannel k8sChannel, TransportClient transportClient) {
        super(K8sChannel.INGRESS_RESOURCE, k8sChannel, transportClient,
                TransportUriMap.get(TransportUriMap.API_K8S_INGRESS));
    }

    @Override
    public void collect() {
        if (k8sChannel == null || k8sChannel.getClient() == null) {
            logger.warn("[INGRESS] k8s client not available");
            return;
        }
        try {
            markAllNotCurrent();
            IngressList list = k8sChannel.getClient().extensions().ingresses().inAnyNamespace().list();
            List<IngressInfo> infos = new ArrayList<>();
            for (Ingress ing : list.getItems()) {
                IngressInfo info = new IngressInfo();
                info.setUid(ing.getMetadata().getUid());
                info.setName(ing.getMetadata().getName());
                info.setNamespace(ing.getMetadata().getNamespace());
                info.setLabels(ing.getMetadata().getLabels());
                info.setAnnotations(ing.getMetadata().getAnnotations());
                info.setExist(true);
                if (ing.getMetadata().getCreationTimestamp() != null) {
                    info.setCreatedTime(ing.getMetadata().getCreationTimestamp());
                }
                info.setRules(buildRules(ing));
                if (hasChanged(info.getUid(), info, info.getName())) {
                    infos.add(info);
                } else {
                    IngressInfo minimal = new IngressInfo();
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
            logger.warn("[INGRESS] collect failed: {}", e.getMessage());
        }
    }

    private List<IngressInfo.IngressRule> buildRules(Ingress ing) {
        List<IngressInfo.IngressRule> rules = new ArrayList<>();
        if (ing.getSpec() == null || ing.getSpec().getRules() == null) return rules;
        for (IngressRule r : ing.getSpec().getRules()) {
            IngressInfo.IngressRule rule = new IngressInfo.IngressRule();
            rule.setHost(r.getHost());
            List<IngressInfo.HttpPath> paths = new ArrayList<>();
            if (r.getHttp() != null && r.getHttp().getPaths() != null) {
                for (HTTPIngressPath p : r.getHttp().getPaths()) {
                    IngressInfo.HttpPath hp = new IngressInfo.HttpPath();
                    hp.setPath(p.getPath());
                    if (p.getBackend() != null) {
                        hp.setServiceName(p.getBackend().getServiceName());
                        if (p.getBackend().getServicePort() != null) {
                            hp.setServicePort(p.getBackend().getServicePort().toString());
                        }
                        hp.setServiceUid(getServiceUidByName(p.getBackend().getServiceName()));
                    }
                    paths.add(hp);
                }
            }
            rule.setPaths(paths);
            rules.add(rule);
        }
        return rules;
    }
}
