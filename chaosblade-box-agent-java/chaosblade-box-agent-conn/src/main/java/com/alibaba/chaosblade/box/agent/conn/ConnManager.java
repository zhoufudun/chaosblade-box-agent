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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages connection handlers. Mirrors Go's conn.Conn struct.
 */
public class ConnManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnManager.class);

    private final ConcurrentHashMap<String, ClientHandle> handlers = new ConcurrentHashMap<>();

    /**
     * Register a named handler.
     */
    public void register(String name, ClientHandle handler) {
        handlers.put(name, handler);
    }

    /**
     * Start all registered handlers concurrently.
     * Each handler is started in its own thread. If any handler fails,
     * the error is logged and the process exits.
     */
    public void start() {
        if (handlers.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ClientHandle> entry : handlers.entrySet()) {
            final String name = entry.getKey();
            final ClientHandle handler = entry.getValue();
            Thread thread = new Thread(() -> {
                logger.info("[conn] starting handler: {}", name);
                try {
                    handler.start();
                } catch (Exception e) {
                    logger.error("[conn] handler '{}' start failed: {}", name, e.getMessage(), e);
                }
            }, "conn-" + name);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Return the registered handlers map (for testing).
     */
    public Map<String, ClientHandle> getHandlers() {
        return handlers;
    }
}
