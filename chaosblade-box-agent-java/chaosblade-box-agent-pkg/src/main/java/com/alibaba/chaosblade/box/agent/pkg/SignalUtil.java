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

package com.alibaba.chaosblade.box.agent.pkg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JVM shutdown hook utilities.
 * Mirrors Go's pkg/tools/signal.go.
 */
public final class SignalUtil {

    private static final Logger logger = LoggerFactory.getLogger(SignalUtil.class);

    private SignalUtil() {
    }

    /**
     * Callback interface for shutdown hooks.
     */
    public interface ShutdownHook {
        void shutdown();
    }

    /**
     * Register JVM shutdown hooks that will be executed on SIGINT/SIGTERM.
     *
     * @param hooks the shutdown hooks to register
     */
    public static void hold(ShutdownHook... hooks) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Received shutdown signal, executing hooks");
            for (ShutdownHook hook : hooks) {
                if (hook == null) {
                    continue;
                }
                try {
                    hook.shutdown();
                } catch (Exception e) {
                    logger.error("Error executing shutdown hook", e);
                }
            }
        }, "chaos-shutdown-hook"));
    }
}
