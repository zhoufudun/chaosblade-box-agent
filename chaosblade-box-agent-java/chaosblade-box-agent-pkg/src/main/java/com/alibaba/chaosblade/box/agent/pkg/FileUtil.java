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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File utilities: existence check, MD5, tar.gz decompression, Gzip compress/decompress.
 * Mirrors Go's pkg/tools/file.go.
 */
public final class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BUFFER_SIZE = 8192;

    private FileUtil() {
    }

    /**
     * Check if a file or directory exists.
     */
    public static boolean isExist(String path) {
        return new File(path).exists();
    }

    /**
     * Decompress a .tar.gz file to the destination directory.
     * Uses raw tar format parsing (JDK only, no external dependencies).
     *
     * @param tarFile  the .tar.gz file path
     * @param destPath the destination directory
     * @throws IOException if decompression fails
     */
    public static void deCompressTgz(String tarFile, String destPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(tarFile);
             GZIPInputStream gzis = new GZIPInputStream(fis)) {
            // Simple tar parser: each entry has a 512-byte header followed by data blocks
            byte[] header = new byte[512];
            while (true) {
                int bytesRead = readFully(gzis, header);
                if (bytesRead < 512) {
                    break;
                }
                // Check for end-of-archive (two consecutive zero blocks)
                if (isZeroBlock(header)) {
                    break;
                }
                // Parse file name (bytes 0-99)
                String name = parseTarString(header, 0, 100);
                if (name.isEmpty()) {
                    break;
                }
                // Check for prefix (bytes 345-499, POSIX/ustar)
                String prefix = parseTarString(header, 345, 155);
                if (!prefix.isEmpty()) {
                    name = prefix + "/" + name;
                }
                // Parse file size (bytes 124-135, octal)
                long size = parseOctal(header, 124, 12);
                // Parse type flag (byte 156)
                byte typeFlag = header[156];

                String fullPath = destPath + "/" + name;

                if (typeFlag == '5' || name.endsWith("/")) {
                    // Directory
                    new File(fullPath).mkdirs();
                } else {
                    // Regular file
                    File outFile = new File(fullPath);
                    File parentDir = outFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        long remaining = size;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buffer.length, remaining);
                            int read = gzis.read(buffer, 0, toRead);
                            if (read < 0) {
                                break;
                            }
                            fos.write(buffer, 0, read);
                            remaining -= read;
                        }
                    }
                    outFile.setExecutable(true, false);
                }
                // Skip padding to 512-byte boundary
                long remainder = size % 512;
                if (remainder != 0) {
                    long skip = 512 - remainder;
                    long skipped = 0;
                    byte[] skipBuf = new byte[(int) skip];
                    while (skipped < skip) {
                        int r = gzis.read(skipBuf, 0, (int) (skip - skipped));
                        if (r < 0) break;
                        skipped += r;
                    }
                }
            }
        }
    }

    private static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int read = in.read(buf, total, buf.length - total);
            if (read < 0) break;
            total += read;
        }
        return total;
    }

    private static boolean isZeroBlock(byte[] block) {
        for (byte b : block) {
            if (b != 0) return false;
        }
        return true;
    }

    private static String parseTarString(byte[] header, int offset, int length) {
        int end = offset;
        for (int i = offset; i < offset + length; i++) {
            if (header[i] == 0) break;
            end = i + 1;
        }
        return new String(header, offset, end - offset, java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    private static long parseOctal(byte[] header, int offset, int length) {
        String s = parseTarString(header, offset, length).trim();
        if (s.isEmpty()) return 0;
        try {
            return Long.parseLong(s, 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Check if a file's MD5 matches the expected value.
     *
     * @param filePath the file path
     * @param md5sum   the expected MD5 hex string
     * @return true if the MD5 matches
     * @throws IOException if the file cannot be read
     */
    public static boolean checkMd5(String filePath, String md5sum) throws IOException {
        String actual = md5sum(filePath);
        return actual.equals(md5sum);
    }

    /**
     * Compute the MD5 hex string of a file.
     */
    public static String md5sum(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return bytesToHex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    /**
     * Compute the MD5 hex string of an object (serialized to JSON).
     */
    public static String md5sumData(Object data) throws IOException {
        if (data == null) {
            throw new IOException("md5 data is null");
        }
        try {
            byte[] jsonBytes = OBJECT_MAPPER.writeValueAsBytes(data);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(jsonBytes);
            return bytesToHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    /**
     * Compress a string using Gzip.
     */
    public static byte[] compressByGzip(String body) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(bos)) {
            gzos.write(body.getBytes("UTF-8"));
        }
        return bos.toByteArray();
    }

    /**
     * Decompress Gzip bytes to a string.
     */
    public static String decompressByGzip(byte[] body) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(body));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Write a string to a file, creating parent directories if needed.
     */
    public static void writeStringToFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
