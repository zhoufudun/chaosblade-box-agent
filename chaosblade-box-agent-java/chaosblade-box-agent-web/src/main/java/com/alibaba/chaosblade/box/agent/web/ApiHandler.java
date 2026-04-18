package com.alibaba.chaosblade.box.agent.web;

import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;

/**
 * Business handler interface for processing API requests.
 * Mirrors Go's web/type.go ApiHandler.
 */
public interface ApiHandler {
    Response handle(Request request);
}
