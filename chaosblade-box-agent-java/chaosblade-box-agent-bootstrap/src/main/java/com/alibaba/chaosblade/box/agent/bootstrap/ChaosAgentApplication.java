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

package com.alibaba.chaosblade.box.agent.bootstrap;

import com.alibaba.chaosblade.box.agent.bootstrap.config.AgentOptions;
import com.alibaba.chaosblade.box.agent.bootstrap.config.BladeOptions;
import com.alibaba.chaosblade.box.agent.collector.K8sChannel;
import com.alibaba.chaosblade.box.agent.conn.CloserHandler;
import com.alibaba.chaosblade.box.agent.conn.ConnConfig;
import com.alibaba.chaosblade.box.agent.conn.ConnManager;
import com.alibaba.chaosblade.box.agent.conn.ConnectHandler;
import com.alibaba.chaosblade.box.agent.conn.HeartbeatHandler;
import com.alibaba.chaosblade.box.agent.conn.MetricHandler;
import com.alibaba.chaosblade.box.agent.pkg.AgentContext;
import com.alibaba.chaosblade.box.agent.pkg.EnvUtil;
import com.alibaba.chaosblade.box.agent.pkg.SignalUtil;
import com.alibaba.chaosblade.box.agent.pkg.UuidUtil;
import com.alibaba.chaosblade.box.agent.transport.HttpTransportChannel;
import com.alibaba.chaosblade.box.agent.transport.TransportClient;
import com.alibaba.chaosblade.box.agent.transport.TransportUriMap;
import com.alibaba.chaosblade.box.agent.web.AgentController;
import com.alibaba.chaosblade.box.agent.web.HandlerRegistrar;
import com.alibaba.chaosblade.box.agent.web.HelmCliClient;
import com.alibaba.chaosblade.box.agent.web.LitmusConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.Enumeration;

/**
 * Spring Boot main application class for ChaosBlade Box Agent.
 * Mirrors Go's cmd/chaos_agent.go main() startup sequence.
 *
 * Startup order:
 * 1. Initialize configuration (AgentOptions)
 * 2. Create HttpTransportChannel
 * 3. Create TransportClient
 * 4. Initialize TransportUriMap
 * 5. Set up K8s client
 * 6. Register connection handlers (ConnectHandler, HeartbeatHandler, MetricHandler)
 * 7. Start ConnManager
 * 8. Register API handlers
 * 9. Start HTTP server (port 19527, handled by Spring Boot)
 */
@SpringBootApplication(scanBasePackages = "com.alibaba.chaosblade.box.agent")
@EnableConfigurationProperties(AgentOptions.class)
public class ChaosAgentApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ChaosAgentApplication.class);
    private static final String PID_FILE = "/var/run/chaos.pid";

    @Autowired
    private AgentOptions agentOptions;

    @Autowired
    private AgentController agentController;

    public static void main(String[] args) {
        SpringApplication.run(ChaosAgentApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            doStartup();
            handleSuccess();
        } catch (Exception e) {
            logger.error("Agent startup failed: {}", e.getMessage(), e);
            handleError(e);
        }
    }

    private void doStartup() throws Exception {
        // 1. Initialize runtime configuration
        initRuntimeConfig();

        // 2. Initialize AgentContext (global options, mirrors Go's options.Opts)
        AgentContext.init(
                agentOptions.getPid(),
                agentOptions.getUid(),
                "",  // cid is set later by ConnectHandler
                AgentOptions.PROGRAM_NAME,
                agentOptions.getVersion(),
                agentOptions.getChaosbladeVersion(),
                agentOptions.getPort()
        );

        // 3. Create HttpTransportChannel
        String endpoint = agentOptions.getTransportEndpoint();
        long timeoutSeconds = agentOptions.getTransportTimeout().getSeconds();
        HttpTransportChannel channel = new HttpTransportChannel(endpoint, timeoutSeconds);
        logger.info("Transport channel created, endpoint={}", endpoint);

        // 4. Create TransportClient (auto-creates interceptor chain)
        TransportClient transportClient = new TransportClient(channel);
        logger.info("Transport client created");

        // 5. Initialize TransportUriMap
        TransportUriMap.init(
                agentOptions.getVpcId(),
                agentOptions.getIp(),
                agentOptions.getPid(),
                AgentOptions.PROGRAM_NAME
        );
        logger.info("Transport URI map initialized");

        // 5. Set up K8s client (returns null if not in K8s)
        K8sChannel k8sChannel = null;
        if (agentOptions.isK8sMode()) {
            k8sChannel = K8sChannel.getInstance();
            if (k8sChannel != null) {
                agentOptions.setClusterIdIfNotPresent(k8sChannel.getClusterId());
                logger.info("K8s client initialized, clusterId={}", k8sChannel.getClusterId());
            } else {
                logger.warn("K8s client initialization failed, running without K8s support");
            }
        }

        // 6. Build ConnConfig from AgentOptions
        ConnConfig connConfig = buildConnConfig();

        // Create connection handlers
        ConnectHandler connectHandler = new ConnectHandler(transportClient, connConfig);
        HeartbeatHandler heartbeatHandler = new HeartbeatHandler(
                transportClient, agentOptions.getHeartbeatPeriod(), connConfig);
        MetricHandler metricHandler = new MetricHandler(transportClient, Duration.ofSeconds(10));

        // Register handlers with ConnManager
        ConnManager connManager = new ConnManager();
        connManager.register(TransportUriMap.API_REGISTRY, connectHandler);
        connManager.register(TransportUriMap.API_HEARTBEAT, heartbeatHandler);
        connManager.register(TransportUriMap.API_METRIC, metricHandler);

        // 7. Start ConnManager (concurrently starts all handlers)
        connManager.start();
        logger.info("ConnManager started");

        // 8. Register API handlers
        HelmCliClient helmClient = new HelmCliClient(
                LitmusConstants.LITMUS_HELM_NAME,
                LitmusConstants.LITMUS_HELM_NAMESPACE
        );

        HandlerRegistrar.registerAll(
                agentController,
                transportClient,
                k8sChannel,
                helmClient,
                BladeOptions.BLADE_BIN_PATH,
                agentOptions.getLitmusChartUrl(),
                () -> EnvUtil.getCurrentDirectory() + File.separator + BladeOptions.CTL_FOR_CHAOS,
                appInfo -> {
                    if (appInfo != null && appInfo.length >= 2) {
                        agentOptions.setApplicationInstance(appInfo[0]);
                        agentOptions.setApplicationGroup(appInfo[1]);
                    }
                }
        );
        logger.info("API handlers registered");

        // 9. HTTP server is started by Spring Boot on port from application.properties

        // Register shutdown hook
        CloserHandler closerHandler = new CloserHandler(transportClient);
        SignalUtil.hold(closerHandler);
        logger.info("Shutdown hook registered");

        logger.info("ChaosBlade Box Agent started successfully on port {}", agentOptions.getPort());
    }

    /**
     * Initialize runtime configuration values (PID, IP, hostname, UID, chaosblade version).
     */
    private void initRuntimeConfig() {
        // PID
        String pid = getProcessId();
        agentOptions.setPid(pid);

        // IP: use configured localIp if set, otherwise auto-detect
        String ip = agentOptions.getLocalIp();
        if (ip == null || ip.isEmpty()) {
            ip = getPrivateIp();
        }
        agentOptions.setIp(ip);
        logger.info("Agent IP: {}", ip);

        // Hostname
        String hostname = EnvUtil.getHostname();
        agentOptions.setHostName(hostname);

        // InstanceId = hostname (matches Go agent behavior)
        agentOptions.setInstanceId(hostname);

        // UID
        String uid = UuidUtil.generateUid();
        agentOptions.setUid(uid);

        // ChaosBlade version
        try {
            File bladeBin = new File(BladeOptions.BLADE_BIN_PATH);
            if (bladeBin.exists()) {
                ProcessBuilder pb = new ProcessBuilder(BladeOptions.BLADE_BIN_PATH, "version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                String version = BladeOptions.parseVersionFromOutput(output.toString());
                agentOptions.setChaosbladeVersion(version);
                logger.info("ChaosBlade version: {}", version);
            }
        } catch (Exception e) {
            logger.warn("Failed to detect ChaosBlade version: {}", e.getMessage());
        }

        logger.info("Runtime config initialized: pid={}, ip={}, hostname={}, uid={}",
                pid, ip, hostname, uid);
    }

    /**
     * Build ConnConfig from AgentOptions.
     */
    private ConnConfig buildConnConfig() {
        ConnConfig config = new ConnConfig();
        config.setIp(agentOptions.getIp());
        config.setPid(agentOptions.getPid());
        config.setUid(agentOptions.getUid());
        config.setInstanceId(agentOptions.getInstanceId());
        config.setNamespace(agentOptions.getNamespace());
        config.setLicense(agentOptions.getLicense());
        config.setStartupMode(agentOptions.getStartupMode());
        config.setVersion(agentOptions.getVersion());
        config.setMode(agentOptions.getMode());
        config.setInstallOperator(agentOptions.getInstallOperator());
        config.setChaosbladeVersion(agentOptions.getChaosbladeVersion());
        config.setClusterId(agentOptions.getClusterId());
        config.setClusterName(agentOptions.getClusterName());
        config.setApplicationInstance(agentOptions.getApplicationInstance());
        config.setApplicationGroup(agentOptions.getApplicationGroup());
        config.setRestrictedVpc(agentOptions.isRestrictedVpc());
        config.setVpcId(agentOptions.getVpcId());
        config.setExternalIpEnable(agentOptions.isExternalIpEnable());
        config.setPort(agentOptions.getPort());
        config.setHeartbeatPeriod(agentOptions.getHeartbeatPeriod());
        return config;
    }

    /**
     * Get the current process ID.
     */
    private static String getProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int atIndex = jvmName.indexOf('@');
        if (atIndex > 0) {
            return jvmName.substring(0, atIndex);
        }
        return jvmName;
    }

    /**
     * Auto-detect private IP address.
     * Mirrors Go's GetPrivateIp() from pkg/options/options.go.
     */
    private static String getPrivateIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            // Fallback
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("Failed to detect private IP: {}", e.getMessage());
            return "127.0.0.1";
        }
    }

    /**
     * Handle successful startup: write PID to file.
     */
    private void handleSuccess() {
        String pid = agentOptions.getPid();
        try {
            writePidFile(pid);
            logger.info("PID {} written to {}", pid, PID_FILE);
        } catch (Exception e) {
            logger.error("Failed to write PID file: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle startup error: write -1 to PID file and exit.
     */
    private void handleError(Exception err) {
        logger.warn("Agent startup failed: {}", err.getMessage());
        try {
            writePidFile("-1");
        } catch (Exception e) {
            logger.error("Failed to write error PID file: {}", e.getMessage());
        }
        System.exit(1);
    }

    /**
     * Write PID value to the PID file.
     */
    private static void writePidFile(String pid) throws Exception {
        File file = new File(PID_FILE);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(pid);
        }
    }
}
