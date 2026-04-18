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

import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Metric handler. Manages metric collectors and schedules periodic reporting.
 * Mirrors Go's conn/metric/metric.go ClientMetricHandler.
 */
public class MetricHandler implements ClientHandle {

    private static final Logger logger = LoggerFactory.getLogger(MetricHandler.class);

    private final TransportClient transportClient;
    private final Duration metricPeriod;
    private final List<MetricCollector> collectors;
    private final List<ScheduledExecutorService> schedulers = new ArrayList<>();

    public MetricHandler(TransportClient transportClient, Duration metricPeriod) {
        this.transportClient = transportClient;
        this.metricPeriod = metricPeriod;
        this.collectors = new ArrayList<>();
    }

    /**
     * Add a metric collector.
     */
    public void addCollector(MetricCollector collector) {
        collectors.add(collector);
    }

    @Override
    public void start() throws Exception {
        for (final MetricCollector collector : collectors) {
            if (!collector.isEnabled()) {
                continue;
            }

            logger.info("[metric] starting collector: {}", collector.getClass().getSimpleName());
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "metric-" + collector.getClass().getSimpleName());
                t.setDaemon(true);
                return t;
            });

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    collector.report();
                } catch (Exception e) {
                    logger.warn("[metric] collector {} report failed: {}",
                            collector.getClass().getSimpleName(), e.getMessage());
                }
            }, 0, metricPeriod.toMillis(), TimeUnit.MILLISECONDS);

            schedulers.add(scheduler);
        }
    }

    @Override
    public void stop() {
        for (ScheduledExecutorService scheduler : schedulers) {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        }
        schedulers.clear();
    }

    /**
     * Return the transport client (for collectors that need it).
     */
    public TransportClient getTransportClient() {
        return transportClient;
    }
}
