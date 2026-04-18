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

import com.alibaba.chaosblade.box.agent.pkg.SignalUtil;
import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import com.alibaba.chaosblade.box.agent.transport.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Close handler. Sends close notification to the server on shutdown.
 * Mirrors Go's conn/close/closer.go ClientCloserHandler.
 */
public class CloserHandler implements SignalUtil.ShutdownHook {

    private static final Logger logger = LoggerFactory.getLogger(CloserHandler.class);

    private final TransportClient transportClient;

    public CloserHandler(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    @Override
    public void shutdown() {
        logger.info("Agent closing");

        // Send close notification asynchronously
        Thread closeThread = new Thread(() -> {
            try {
                logger.info("Invoking chaos service to close");
                Request request = Request.newDefaultRequest();
                Uri uri = TransportUriMap.get(TransportUriMap.API_CLOSE);
                Response response = transportClient.invoke(uri, request, true);
                if (!response.isSuccess()) {
                    logger.warn("Invoking chaos service close failed: {}", response.getError());
                }
            } catch (Exception e) {
                logger.warn("Invoking chaos service close error: {}", e.getMessage());
            }
        }, "closer-notify");
        closeThread.setDaemon(true);
        closeThread.start();

        // Wait 2 seconds for the close notification to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Agent closed");
    }
}
