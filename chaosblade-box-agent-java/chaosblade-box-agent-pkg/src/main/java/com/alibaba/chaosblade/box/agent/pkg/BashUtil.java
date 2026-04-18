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
import java.util.concurrent.TimeUnit;

/**
 * Shell command execution utility.
 * Mirrors Go's pkg/bash/bash.go.
 */
public final class BashUtil {

    private static final Logger logger = LoggerFactory.getLogger(BashUtil.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 60;

    private BashUtil() {
    }

    /**
     * Result of a script execution.
     */
    public static class ExecResult {
        private final String output;
        private final String errorMsg;
        private final boolean success;

        public ExecResult(String output, String errorMsg, boolean success) {
            this.output = output;
            this.errorMsg = errorMsg;
            this.success = success;
        }

        public String getOutput() { return output; }
        public String getErrorMsg() { return errorMsg; }
        public boolean isSuccess() { return success; }
    }

    /**
     * Execute a shell script with arguments. Default timeout is 60 seconds.
     *
     * @param script the script path to execute
     * @param args   the arguments to pass
     * @return the execution result
     */
    public static ExecResult execScript(String script, String args) {
        return execScript(script, args, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Execute a shell script with arguments and a custom timeout.
     *
     * @param script         the script path to execute
     * @param args           the arguments to pass
     * @param timeoutSeconds the timeout in seconds
     * @return the execution result
     */
    public static ExecResult execScript(String script, String args, long timeoutSeconds) {
        logger.info("[bash] ExecScript called, script: {}, args: {}", script, args);

        if (!new File(script).exists()) {
            logger.warn("[bash] Script file not found: {}", script);
            return new ExecResult("", script + " not found", false);
        }

        try {
            String command = script + " " + args;
            ProcessBuilder pb;
            if (EnvUtil.isWindows()) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", command);
            }
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("[bash] Command timed out after {}s", timeoutSeconds);
                return new ExecResult(output.toString(), "timeout after " + timeoutSeconds + "s", false);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.warn("[bash] Command exited with code: {}", exitCode);
                return new ExecResult(output.toString(), "exit code: " + exitCode, false);
            }

            logger.info("[bash] Command completed successfully, output length: {}", output.length());
            return new ExecResult(output.toString(), "", true);
        } catch (Exception e) {
            logger.error("[bash] Command execution failed", e);
            return new ExecResult("", e.getMessage(), false);
        }
    }
}
