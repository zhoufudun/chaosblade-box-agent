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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP file download utility.
 * Mirrors Go's pkg/tools/download.go.
 */
public final class DownloadUtil {

    private static final Logger logger = LoggerFactory.getLogger(DownloadUtil.class);
    private static final int BUFFER_SIZE = 8192;

    private DownloadUtil() {
    }

    /**
     * Download a file from the given URL to the destination path.
     *
     * @param destFileFullPath the local file path to save to
     * @param url              the URL to download from
     * @throws IOException if the download fails
     */
    public static void download(String destFileFullPath, String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("response code: " + responseCode);
            }

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(destFileFullPath)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Set file executable (like Go's os.Chmod 0744)
            java.io.File file = new java.io.File(destFileFullPath);
            file.setExecutable(true, false);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
