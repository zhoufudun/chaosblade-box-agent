package com.alibaba.chaosblade.box.agent.collector;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceInfo extends CommonInfo {
    // Namespace only has CommonInfo fields
}
