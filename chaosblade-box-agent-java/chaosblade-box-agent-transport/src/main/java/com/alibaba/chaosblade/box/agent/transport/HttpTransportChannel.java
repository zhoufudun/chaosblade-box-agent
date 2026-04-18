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

package com.alibaba.chaosblade.box.agent.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP-based transport channel using OkHttp.
 * Mirrors Go's pkg/http/http.go HttpClient.
 */
public class HttpTransportChannel implements TransportChannel {

    private static final Logger logger = LoggerFactory.getLogger(HttpTransportChannel.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient client;
    private final String baseUrl;

    /**
     * Create an HTTP transport channel.
     *
     * @param endpoint host:port of the remote server
     * @param timeoutSeconds connection and read timeout in seconds
     */
    public HttpTransportChannel(String endpoint, long timeoutSeconds) {
        this.baseUrl = "http://" + endpoint;
        this.client = buildClient(timeoutSeconds);
    }

    private static OkHttpClient buildClient(long timeoutSeconds) {
        try {
            // Trust all certs (InsecureSkipVerify equivalent)
            X509TrustManager trustAllManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), trustAllManager)
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            logger.warn("Failed to create TLS-insecure OkHttpClient, falling back to default", e);
            return new OkHttpClient.Builder()
                    .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();
        }
    }

    @Override
    public String doInvoker(Uri uri, String jsonParam) throws IOException {
        // Deserialize to Request, then merge to body
        Request request = objectMapper.readValue(jsonParam, Request.class);
        Map<String, String> body = request.getBody();
        byte[] reqBody = objectMapper.writeValueAsBytes(body);

        String url = baseUrl + "/" + uri.getHandlerName();
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(url)
                .post(RequestBody.create(reqBody, JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .build();

        try (okhttp3.Response response = client.newCall(httpRequest).execute()) {
            if (response.body() == null) {
                throw new IOException("Empty response body from " + uri.getHandlerName());
            }
            String result = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException(String.format(
                        "HTTP call %s failed, code: %d, body: %s",
                        uri.getHandlerName(), response.code(), result));
            }
            return result;
        }
    }
}
