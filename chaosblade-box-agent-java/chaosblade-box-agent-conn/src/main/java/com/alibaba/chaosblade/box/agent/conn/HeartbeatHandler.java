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

import com.alibaba.chaosblade.box.agent.pkg.LimitedList;
import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import com.alibaba.chaosblade.box.agent.transport.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Heartbeat handler. Sends periodic heartbeats to the server.
 * Mirrors Go's conn/heartbeat/heartbeat.go ClientHeartbeatHandler.
 */
public class HeartbeatHandler implements ClientHandle {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    /** Global heartbeat snapshot list, capacity 26. Accessible by Monitor module. */
    public static final LimitedList<HBSnapshot> HB_SNAPSHOT_LIST = new LimitedList<>(26);

    private final TransportClient transportClient;
    private final Duration period;
    private final ConnConfig config;
    private ScheduledExecutorService scheduler;

    public HeartbeatHandler(TransportClient transportClient, Duration period, ConnConfig config) {
        this.transportClient = transportClient;
        this.period = period;
        this.config = config;
    }

    @Override
    public void start() throws Exception {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::sendHeartbeat, period.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        logger.info("[heartbeat] started with period {}ms", period.toMillis());
    }

    @Override
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void sendHeartbeat() {
        try {
            Request request = Request.newRequest(
                    config.getPid(),
                    config.getUid(),
                    config.getCid(),
                    "CHAOS_AGENT",
                    config.getVersion(),
                    config.getChaosbladeVersion(),
                    config.getPort()
            );

            if (config.isExternalIpEnable()) {
                request.addParam("ip", config.getIp());
            }
            request.addParam("appInstance", config.getApplicationInstance());
            request.addParam("appGroup", config.getApplicationGroup());

            Uri uri = TransportUriMap.get(TransportUriMap.API_HEARTBEAT);
            Response response = transportClient.invoke(uri, request, true);

            if (!response.isSuccess()) {
                logger.error("[heartbeat] send failed: {}", response.getError());
                record(false);
                return;
            }

            logger.info("[heartbeat] success");
            record(true);
        } catch (Exception e) {
            logger.error("[heartbeat] send failed: {}", e.getMessage());
            record(false);
        }
    }

    private void record(boolean success) {
        HB_SNAPSHOT_LIST.put(new HBSnapshot(success));
    }
}
