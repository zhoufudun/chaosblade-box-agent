/*
 * Copyright 2025 The ChaosBlade Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.chaosblade.box.agent.conn;

import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import com.alibaba.chaosblade.box.agent.transport.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback handler for upgrade notifications.
 * Mirrors Go's conn/callback/callback.go CallbackHandler.
 */
public class CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    private final TransportClient transportClient;

    public CallbackHandler(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    /**
     * Send an upgrade callback to the server.
     *
     * @param status       the upgrade status code
     * @param oldVersion   the previous version
     * @param newVersion   the target version
     * @param currVersion  the current version after upgrade
     * @param message      optional message
     * @param programType  the program type (e.g. "CHAOS_AGENT", "CHAOS_BLADE")
     */
    public void callback(int status, String oldVersion, String newVersion,
                         String currVersion, String message, String programType) {
        Uri uri = TransportUriMap.get(TransportUriMap.API_UPGRADE_CALLBACK);
        if (uri == null) {
            return;
        }

        Request request = Request.newDefaultRequest();
        request.addParam("oldVersion", oldVersion);
        request.addParam("newVersion", newVersion);
        request.addParam("currVersion", currVersion);
        request.addParam("status", String.valueOf(status));
        request.addParam("message", message);
        request.addParam("type", programType);

        try {
            Response response = transportClient.invoke(uri, request, true);
            if (!response.isSuccess()) {
                logger.warn("[callback] invoke upgrade callback failed: {}", response.getError());
            }
        } catch (Exception e) {
            logger.warn("[callback] invoke upgrade callback error: {}", e.getMessage());
        }
    }
}
