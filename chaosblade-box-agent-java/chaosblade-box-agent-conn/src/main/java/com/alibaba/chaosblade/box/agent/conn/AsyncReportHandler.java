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
import com.alibaba.chaosblade.box.agent.transport.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Async report handler for chaos tool execution status.
 * Mirrors Go's conn/asyncreport/asyncreport.go AsyncReportHandler.
 */
public class AsyncReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(AsyncReportHandler.class);

    private final TransportClient transportClient;

    public AsyncReportHandler(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    /**
     * Report the status of a chaos experiment execution.
     *
     * @param uid      the experiment UID
     * @param status   the execution status
     * @param errorMsg optional error message (may be null or empty)
     * @param toolType optional tool type (may be null or empty)
     * @param uri      the target URI for reporting
     */
    public void reportStatus(String uid, String status, String errorMsg, String toolType, Uri uri) {
        String recordMsg = "uid: " + uid + ", status: " + status;

        Request request = Request.newDefaultRequest();
        request.addParam("uid", uid);
        request.addParam("status", status);

        if (errorMsg != null && !errorMsg.isEmpty()) {
            request.addParam("error", errorMsg);
        }
        if (toolType != null && !toolType.isEmpty()) {
            request.addParam("ToolType", toolType);
        }

        logger.info("[asyncReport] reporting status: {}", recordMsg);

        try {
            Response response = transportClient.invoke(uri, request, true);
            if (!response.isSuccess()) {
                logger.warn("[asyncReport] report status failed: {}, {}", response.getError(), recordMsg);
                return;
            }
            logger.info("[asyncReport] report status success: {}", recordMsg);
        } catch (Exception e) {
            logger.warn("[asyncReport] report status error: {}, {}", e.getMessage(), recordMsg);
        }
    }
}
