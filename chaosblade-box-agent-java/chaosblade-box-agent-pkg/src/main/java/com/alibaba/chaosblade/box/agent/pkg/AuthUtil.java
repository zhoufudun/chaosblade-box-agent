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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Authentication utility: SHA256+Base64 signing, AK/SK file read/write with ReadWriteLock.
 * Mirrors Go's pkg/tools/auth.go.
 */
public final class AuthUtil {

    private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);

    public static final String ACCESS_KEY_NAME = "AK";
    public static final String SECRET_KEY_NAME = "SK";
    public static final String DELIMITER = "=";

    public static final String APP_INSTANCE_KEY_NAME = "appInstance";
    public static final String APP_GROUP_KEY_NAME = "appGroup";

    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static volatile String localAccessKey = "";
    private static volatile String localSecureKey = "";

    /** Path to the .chaos.app file, defaults to current directory */
    private static volatile String appFilePath = EnvUtil.getCurrentDirectory() + File.separator + ".chaos.app";

    private AuthUtil() {
    }

    // --- Accessors ---

    public static String getAccessKey() {
        rwLock.readLock().lock();
        try {
            return localAccessKey;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static String getSecureKey() {
        rwLock.readLock().lock();
        try {
            return localSecureKey;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static String getAppFilePath() {
        return appFilePath;
    }

    public static void setAppFilePath(String path) {
        appFilePath = path;
    }

    // --- Signing ---

    /**
     * Generate a SHA256+Base64 signature for the given data using the current SecretKey.
     *
     * @param signData the data to sign
     * @return the Base64-encoded signature
     */
    public static String sign(String signData) {
        return signWithKey(signData, localSecureKey);
    }

    /**
     * Generate a SHA256+Base64 signature for the given data using the specified SecretKey.
     *
     * @param signData  the data to sign
     * @param secretKey the secret key
     * @return the Base64-encoded signature
     */
    public static String signWithKey(String signData, String secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((signData + secretKey).getBytes(StandardCharsets.UTF_8));
            // Convert hash to hex string, then Base64 encode
            String hexString = bytesToHex(hash);
            return java.util.Base64.getEncoder().encodeToString(hexString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Sign failed", e);
            return "";
        }
    }

    /**
     * Verify a signature against the expected signature for the given data.
     *
     * @param receivedSign the received signature
     * @param signData     the data that was signed
     * @return true if the signature matches
     */
    public static boolean auth(String receivedSign, String signData) {
        String expectedSign = sign(signData);
        if (!expectedSign.equals(receivedSign)) {
            logger.warn("Sign not equal. ak: {}, expectSign: {}, receiveSign: {}",
                    getAccessKey(), expectedSign, receivedSign);
            return false;
        }
        return true;
    }

    /**
     * Verify a signature using a specific secret key.
     */
    public static boolean authWithKey(String receivedSign, String signData, String secretKey) {
        String expectedSign = signWithKey(signData, secretKey);
        return expectedSign.equals(receivedSign);
    }

    // --- AK/SK File Operations ---

    /**
     * Record AK/SK to the cert file (~/.chaos.cert) and update in-memory keys.
     *
     * @param accessKey the access key
     * @param secretKey the secret key
     * @throws IOException if writing fails
     */
    public static void recordSecretKeyToFile(String accessKey, String secretKey) throws IOException {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            logger.warn("accessKey or secretKey is empty: ak={}, sk={}", accessKey, secretKey);
            throw new IllegalArgumentException("accessKey or secretKey is empty");
        }

        Map<String, String> keys = new HashMap<>();
        keys.put(ACCESS_KEY_NAME, accessKey);
        keys.put(SECRET_KEY_NAME, secretKey);

        String certPath = EnvUtil.getUserHome() + File.separator + ".chaos.cert";
        recordMapToFile(keys, certPath, true);

        rwLock.writeLock().lock();
        try {
            localAccessKey = accessKey;
            localSecureKey = secretKey;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Set AK/SK in memory without writing to file.
     */
    public static void setKeys(String accessKey, String secretKey) {
        rwLock.writeLock().lock();
        try {
            localAccessKey = accessKey;
            localSecureKey = secretKey;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Record application info (appInstance, appGroup) to the .chaos.app file.
     *
     * @param appInstance the application instance name
     * @param appGroup    the application group name
     * @param truncate    whether to truncate the file before writing
     * @throws IOException if writing fails
     */
    public static void recordApplicationToFile(String appInstance, String appGroup, boolean truncate) throws IOException {
        Map<String, String> keys = new HashMap<>();
        keys.put(APP_INSTANCE_KEY_NAME, appInstance);
        keys.put(APP_GROUP_KEY_NAME, appGroup);
        recordMapToFile(keys, appFilePath, truncate);
    }

    /**
     * Read application info from the .chaos.app file.
     *
     * @return a String array of [appInstance, appGroup]
     * @throws IOException if reading fails
     */
    public static String[] readAppInfoFromFile() throws IOException {
        return readAppInfoFromFile(appFilePath);
    }

    /**
     * Read application info from a specific file path.
     *
     * @param filePath the file to read from
     * @return a String array of [appInstance, appGroup]
     * @throws IOException if reading fails
     */
    public static String[] readAppInfoFromFile(String filePath) throws IOException {
        String appInstance = "";
        String appGroup = "";

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                int delimIdx = line.indexOf(DELIMITER);
                if (delimIdx < 0) {
                    continue;
                }
                String key = line.substring(0, delimIdx);
                String value = line.substring(delimIdx + 1);
                if (APP_INSTANCE_KEY_NAME.equals(key)) {
                    appInstance = value;
                } else if (APP_GROUP_KEY_NAME.equals(key)) {
                    appGroup = value;
                }
            }
        } finally {
            reader.close();
        }

        return new String[]{appInstance, appGroup};
    }

    /**
     * Write a map of key-value pairs to a file.
     *
     * @param data     the key-value pairs
     * @param filePath the file path
     * @param truncate whether to truncate the file before writing
     * @throws IOException if writing fails
     */
    public static void recordMapToFile(Map<String, String> data, String filePath, boolean truncate) throws IOException {
        if (data == null || data.isEmpty()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            // Ensure parent directory exists
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, !truncate))) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    writer.write(entry.getKey() + DELIMITER + entry.getValue());
                    writer.newLine();
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // --- Internal ---

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
