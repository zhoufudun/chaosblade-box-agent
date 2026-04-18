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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Environment utilities.
 * Mirrors Go's pkg/tools/env.go.
 */
public final class EnvUtil {

    private static final Logger logger = LoggerFactory.getLogger(EnvUtil.class);

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);

    private static volatile String chaosPath;
    private static volatile String metricPath;
    private static volatile String agentPath;

    private EnvUtil() {
    }

    /**
     * Return user home directory.
     */
    public static String getUserHome() {
        String home = System.getProperty("user.home");
        return home != null ? home : "/root";
    }

    /**
     * Return the current process directory (working directory).
     */
    public static String getCurrentDirectory() {
        if (chaosPath != null && !chaosPath.isEmpty()) {
            return chaosPath;
        }
        String dir = System.getProperty("user.dir");
        if (dir == null || dir.isEmpty()) {
            logger.error("Cannot get the process path");
            dir = ".";
        }
        chaosPath = dir;
        return dir;
    }

    /**
     * Set the chaos path explicitly.
     */
    public static void setChaosPath(String path) {
        chaosPath = path;
    }

    /**
     * Return the hostname.
     */
    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            logger.warn("Failed to get hostname", e);
            return "unknown";
        }
    }

    /**
     * Return system uptime string.
     */
    public static String getUptime() {
        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder("cmd.exe", "/c", "net stats srv");
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", "uptime");
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "";
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String output = sb.toString().trim();
            if (output.isEmpty()) {
                return "";
            }
            // On Linux, uptime output is like "10:30:00 up 5 days, ..."
            // Return the part before the first comma
            int commaIdx = output.indexOf(',');
            if (commaIdx > 0) {
                return output.substring(0, commaIdx);
            }
            return output;
        } catch (Exception e) {
            // Try reading /proc/uptime on Linux
            try {
                File procUptime = new File("/proc/uptime");
                if (procUptime.exists()) {
                    java.util.Scanner scanner = new java.util.Scanner(procUptime);
                    if (scanner.hasNext()) {
                        String val = scanner.next();
                        scanner.close();
                        return val;
                    }
                    scanner.close();
                }
            } catch (Exception ex) {
                // ignore
            }
            return "";
        }
    }

    /**
     * Check if the OS is Unix-like (Linux or macOS).
     */
    public static boolean isUnix() {
        return OS_NAME.contains("linux") || OS_NAME.contains("mac") || OS_NAME.contains("darwin");
    }

    /**
     * Check if the OS is Windows.
     */
    public static boolean isWindows() {
        return OS_NAME.contains("windows");
    }

    /**
     * Return the metric directory path, creating it if necessary.
     */
    public static String getMetricDirectory() {
        if (metricPath != null && !metricPath.isEmpty()) {
            return metricPath;
        }
        String path = getCurrentDirectory() + File.separator + "metric";
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logger.error("Cannot create metric path: {}", path);
                return getCurrentDirectory();
            }
        }
        metricPath = path;
        return path;
    }

    /**
     * Return the agent directory path, creating it if necessary.
     */
    public static String getAgentDirectory() {
        if (agentPath != null && !agentPath.isEmpty()) {
            return agentPath;
        }
        String path = getCurrentDirectory() + File.separator + "agent";
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logger.error("Cannot create agent path: {}", path);
                return getCurrentDirectory();
            }
        }
        agentPath = path;
        return path;
    }

    /**
     * Check if the region is public environment.
     */
    public static boolean isPublicEnv(String regionId) {
        return "cn-public".equals(regionId);
    }
}
