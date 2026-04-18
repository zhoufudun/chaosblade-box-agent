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

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID generation utilities.
 * Mirrors Go's pkg/tools/uuid.go.
 */
public final class UuidUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidUtil() {
    }

    /**
     * Generate a UUID v4 string.
     */
    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a 16-character hexadecimal random UID.
     */
    public static String generateUid() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(16);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
