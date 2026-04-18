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

package com.alibaba.chaosblade.box.agent.bootstrap.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Blade CLI path constants, operation maps, and version parsing.
 * Mirrors the Go BladeOptions from pkg/options/bladeoptions.go and chaostoolsoptions.go.
 */
public final class BladeOptions {

    private BladeOptions() {
        // utility class
    }

    // --- Path constants ---
    public static final String BLADE_BIN = "blade";
    public static final String BLADE_DIR_NAME = "chaosblade";
    public static final String BLADE_DAT_FILE_NAME = "chaosblade.dat";
    public static final String BLADE_BAK_DAT_FILE_NAME = "chaosblade.dat.bak";
    public static final String CTL_FOR_CHAOS = "chaosctl.sh";

    public static final String BLADE_HOME = "/opt/" + BLADE_DIR_NAME;
    public static final String BLADE_BIN_PATH = BLADE_HOME + "/" + BLADE_BIN;

    // --- Operation maps ---
    public static final Set<String> PREPARE_OPERATION = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("prepare", "p")));

    public static final Set<String> CREATE_OPERATION = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("create", "c")));

    public static final Set<String> REVOKE_OPERATION = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("revoke", "r")));

    public static final Set<String> DESTROY_OPERATION = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("destroy", "d")));

    public static final Set<String> ASYNC_PARAMETER = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("async", "a")));

    /**
     * Parse version number from blade CLI output.
     * Supports two formats:
     * <ul>
     *   <li>"Version:     1.8.0" (with possible extra whitespace)</li>
     *   <li>"version: 1.7.3" (lowercase)</li>
     * </ul>
     *
     * @param output the raw output from "blade version" command
     * @return the parsed version string (e.g. "1.8.0")
     * @throws IllegalArgumentException if version cannot be parsed
     */
    public static String parseVersionFromOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            throw new IllegalArgumentException("cannot get blade version: empty output");
        }

        String[] lines = output.trim().split("\n");

        // First pass: try "Version:" prefix (case-sensitive, original format)
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("Version:")) {
                String version = extractVersionAfterColon(line);
                if (version != null) {
                    return version;
                }
            }
        }

        // Second pass: try "version:" prefix (case-insensitive)
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.toLowerCase().startsWith("version:")) {
                String version = extractVersionAfterColon(line);
                if (version != null) {
                    return version;
                }
            }
        }

        throw new IllegalArgumentException("cannot parse version info from output: " + output);
    }

    /**
     * Extract the version string after the colon in a "Version: X.Y.Z" line.
     * Takes only the first whitespace-delimited token after the colon.
     */
    private static String extractVersionAfterColon(String line) {
        int colonIdx = line.indexOf(':');
        if (colonIdx < 0 || colonIdx == line.length() - 1) {
            return null;
        }
        String afterColon = line.substring(colonIdx + 1).trim();
        if (afterColon.isEmpty()) {
            return null;
        }
        // Take only the first word (the version number)
        String[] parts = afterColon.split("\\s+");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            return parts[0];
        }
        return null;
    }
}
