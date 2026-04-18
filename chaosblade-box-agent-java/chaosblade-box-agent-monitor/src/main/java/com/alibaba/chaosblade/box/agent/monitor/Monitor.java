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

package com.alibaba.chaosblade.box.agent.monitor;

import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import com.alibaba.chaosblade.box.agent.transport.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Health monitor that periodically checks heartbeat status and sends
 * start/stop events to the server.
 * Singleton pattern, mirrors Go's monitor/monitor.go.
 */
public class Monitor {

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);
    private static final long MONITOR_INTERVAL_SEC = 10;

    private static volatile Monitor instance;
    private static final Object LOCK = new Object();

    private final Checker checker;
    private final TransportClient transportClient;
    private ScheduledExecutorService scheduler;

    private Monitor(Checker checker, TransportClient transportClient) {
        this.checker = checker;
        this.transportClient = transportClient;
    }

    /**
     * Get or create the singleton Monitor instance.
     */
    public static Monitor getInstance(TransportClient transportClient) {
        if (instance != null) {
            return instance;
        }
        synchronized (LOCK) {
            if (instance == null) {
                instance = new Monitor(new DefaultChecker(), transportClient);
            }
        }
        return instance;
    }

    /**
     * Start the periodic monitor loop.
     */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "monitor-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::doMonitor, MONITOR_INTERVAL_SEC,
                MONITOR_INTERVAL_SEC, TimeUnit.SECONDS);
        logger.info("[Monitor] started with interval {}s", MONITOR_INTERVAL_SEC);
    }

    /**
     * Stop the monitor scheduler.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void doMonitor() {
        try {
            MonitorAction action = checker.check();

            if (action.isNeedStop()) {
                String msg = String.format("monitor exception[%s], stop", action.getReason());
                stopWithReason(msg);
            }

            if (action.isNeedExit()) {
                logger.warn("monitor error[{}], exit", action.getReason());
                // Send SIGTERM equivalent - trigger shutdown hooks
                System.exit(5);
            }

            if (action.isNeedStart()) {
                String msg = String.format("recover[%s], start", action.getReason());
                startWithReason(msg);
            }
        } catch (Exception e) {
            logger.error("[Monitor] check error: {}", e.getMessage());
        }
    }

    private void stopWithReason(String reason) {
        logger.warn("[Controller] send stop event to server, reason: {}", reason);
        sendEventToServer("stop", reason);
    }

    private void startWithReason(String reason) {
        logger.info("[Controller] send start event to server, reason: {}", reason);
        sendEventToServer("start", reason);
    }

    private void sendEventToServer(String event, String reason) {
        try {
            Uri uri = TransportUriMap.get(TransportUriMap.API_EVENT);
            if (uri == null) {
                return;
            }
            Request request = Request.newDefaultRequest();
            request.addParam("event", event);
            request.addParam("reason", reason);
            transportClient.invoke(uri, request, true);
            logger.info("[Monitor] send {} event with {} reason to server successfully.", event, reason);
        } catch (Exception e) {
            logger.warn("[Monitor] send {} event with {} reason to server error {}.", event, reason, e.getMessage());
        }
    }

    /** Reset singleton for testing purposes. */
    static void resetInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}
