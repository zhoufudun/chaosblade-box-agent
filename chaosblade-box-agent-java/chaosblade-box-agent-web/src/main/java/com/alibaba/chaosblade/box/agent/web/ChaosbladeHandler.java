package com.alibaba.chaosblade.box.agent.web;

import com.alibaba.chaosblade.box.agent.conn.AsyncReportHandler;
import com.alibaba.chaosblade.box.agent.pkg.BashUtil;
import com.alibaba.chaosblade.box.agent.pkg.FileUtil;
import com.alibaba.chaosblade.box.agent.transport.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles chaosblade CLI command execution.
 * Mirrors Go's web/handler/chaosblade.go ChaosbladeHandler.
 */
public class ChaosbladeHandler implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChaosbladeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long DEFAULT_TIMEOUT = 60;

    private static final Set<String> CREATE_OPS = new HashSet<>(Arrays.asList("create"));
    private static final Set<String> PREPARE_OPS = new HashSet<>(Arrays.asList("prepare"));
    private static final Set<String> DESTROY_OPS = new HashSet<>(Arrays.asList("destroy"));
    private static final Set<String> REVOKE_OPS = new HashSet<>(Arrays.asList("revoke"));

    private static final String JAVA_TYPE = "jvm";

    /** Running experiments: uid → cmdline */
    private final ConcurrentHashMap<String, String> running = new ConcurrentHashMap<>();
    private final TransportClient transportClient;
    private final String bladeBinPath;

    public ChaosbladeHandler(TransportClient transportClient, String bladeBinPath) {
        this.transportClient = transportClient;
        this.bladeBinPath = bladeBinPath;
    }

    @Override
    public Response handle(Request request) {
        Map<String, String> params = request.getParams();
        if (params == null) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "cmd");
        }
        String cmd = params.get("cmd");
        if (cmd == null || cmd.isEmpty()) {
            return Response.returnFail(ErrorCodes.PARAMETER_EMPTY, "cmd");
        }
        logger.info("[chaosblade] executing cmd: {}", cmd);
        return exec(cmd);
    }

    private Response exec(String cmd) {
        String[] fields = cmd.trim().split("\\s+");
        if (fields.length == 0) {
            return Response.returnFail(ErrorCodes.PARAMETER_LESS, "command");
        }

        if (!FileUtil.isExist(bladeBinPath)) {
            return Response.returnFail(ErrorCodes.CHAOSBLADE_FILE_NOT_FOUND);
        }

        String command = fields[0];
        BashUtil.ExecResult result = BashUtil.execScript(bladeBinPath, cmd, DEFAULT_TIMEOUT);
        logger.info("[chaosblade] result: {}, error: {}, success: {}", result.getOutput(), result.getErrorMsg(), result.isSuccess());

        if (result.isSuccess()) {
            String output = result.getOutput();
            if (output == null || output.trim().isEmpty()) {
                return Response.returnFail(ErrorCodes.SERVER_ERROR, "blade returned empty output");
            }
            Response response = parseResult(output);
            if (!response.isSuccess()) {
                logger.warn("[chaosblade] execute failed, result: {}", output);
                if (isK8sCreateCmd(cmd, command)) {
                    String uid = extractUidFromResponse(response, output);
                    if (uid != null && !uid.isEmpty()) {
                        waitForK8sStatus(uid);
                    }
                }
                return response;
            }

            // K8s create: wait for operator
            if (isK8sCreateCmd(cmd, command)) {
                String uid = extractUidFromResponse(response, output);
                if (uid != null && !uid.isEmpty()) {
                    waitForK8sStatus(uid);
                }
            }

            // Cache and safe point
            String arg = fields.length > 1 ? fields[1] : "";
            handleCacheAndSafePoint(cmd, command, arg, response);
            return response;
        } else {
            String output = result.getOutput();
            if (output != null && !output.trim().isEmpty()) {
                try {
                    Response response = objectMapper.readValue(output, Response.class);
                    if (isK8sCreateCmd(cmd, command)) {
                        String uid = extractUidFromResponse(response, output);
                        if (uid != null && !uid.isEmpty()) {
                            waitForK8sStatus(uid);
                        }
                    }
                    return response;
                } catch (Exception e) {
                    logger.warn("[chaosblade] unmarshal error result failed: {}", e.getMessage());
                    if (isK8sCreateCmd(cmd, command)) {
                        String uid = extractUidFromRawResult(output);
                        if (uid != null && !uid.isEmpty()) {
                            waitForK8sStatus(uid);
                        }
                    }
                }
            }
            String errMsg = result.getErrorMsg();
            return Response.returnFail(ErrorCodes.RESULT_UNMARSHAL_FAILED,
                    output != null ? output : "", errMsg != null ? errMsg : "");
        }
    }

    /**
     * Parse blade CLI output to Response. Handles prefixed log output by finding first '{'.
     */
    static Response parseResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            return Response.returnFail(ErrorCodes.SERVER_ERROR, "empty result");
        }
        try {
            return objectMapper.readValue(result, Response.class);
        } catch (Exception e) {
            int idx = result.indexOf('{');
            if (idx < 0) {
                return Response.returnFail(ErrorCodes.SERVER_ERROR,
                        "execute success, but parse result err, result: " + result);
            }
            try {
                return objectMapper.readValue(result.substring(idx), Response.class);
            } catch (Exception ex) {
                return Response.returnFail(ErrorCodes.SERVER_ERROR,
                        "execute success, but unmarshal result err, result: " + result);
            }
        }
    }

    /**
     * Cache running experiments and handle safe points.
     * Mirrors Go's handleCacheAndSafePoint.
     */
    private void handleCacheAndSafePoint(String cmdline, String command, String arg, Response response) {
        try {
            if (isCreateOrPrepare(command)) {
                String uid = extractUid(response);
                if (uid != null && !uid.isEmpty()) {
                    running.put(uid, cmdline);

                    // Java agent install check
                    if (PREPARE_OPS.contains(command) && JAVA_TYPE.equals(arg)) {
                        asyncCheckAndReport(uid, TransportUriMap.API_JAVA_INSTALL, true);
                    }
                    // Async create check
                    if (isAsyncCreate(cmdline)) {
                        asyncCheckAndReport(uid, TransportUriMap.API_CHAOSBLADE_ASYNC, false);
                    }
                }
            } else if (isDestroyOrRevoke(command)) {
                String uid = arg;
                running.remove(uid);

                // For revoke, query preparation status and report java agent uninstall
                if (REVOKE_OPS.contains(command)) {
                    queryAndReportRevoke(uid);
                }
            }
        } catch (Exception e) {
            logger.warn("[chaosblade] handleCacheAndSafePoint error: {}", e.getMessage());
        }
    }

    /**
     * Query preparation status for revoke command and report java agent uninstall if needed.
     */
    private void queryAndReportRevoke(String uid) {
        Thread thread = new Thread(() -> {
            try {
                Preparation record = queryPreparationStatus(uid);
                if (record == null) {
                    logger.warn("[chaosblade] preparation record not found, uid: {}", uid);
                    return;
                }
                if (JAVA_TYPE.equals(record.programType)) {
                    // Check and report java agent uninstall status
                    String status = "Unknown";
                    String errorMsg = "";
                    try {
                        String[] result = timingCheckStatus(uid);
                        status = result[0];
                        errorMsg = result[1];
                    } catch (Exception e) {
                        status = "Error";
                        errorMsg = e.getMessage();
                    }
                    reportStatus(uid, status, errorMsg, TransportUriMap.API_JAVA_UNINSTALL);
                }
            } catch (Exception e) {
                logger.warn("[chaosblade] queryAndReportRevoke error: {}", e.getMessage());
            }
        }, "revoke-check-" + uid);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Async check status and report. Mirrors Go's checkAndReportJavaAgentStatus / checkAndReportAsyncStatus.
     */
    private void asyncCheckAndReport(String uid, String apiName, boolean deleteOnError) {
        Thread thread = new Thread(() -> {
            try {
                String[] result = timingCheckStatus(uid);
                String status = result[0];
                String errorMsg = result[1];

                // Delete callback if error
                if (deleteOnError && "Error".equalsIgnoreCase(status)) {
                    running.remove(uid);
                }

                reportStatus(uid, status, errorMsg, apiName);
            } catch (Exception e) {
                logger.warn("[chaosblade] asyncCheckAndReport error: {}", e.getMessage());
            }
        }, "async-check-" + uid);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Periodically check status until it's no longer "Created" or timeout.
     * Returns [status, errorMsg].
     * Mirrors Go's timingCheckStatus.
     */
    private String[] timingCheckStatus(String uid) {
        String status = "Unknown";
        String errorMsg = "";

        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new String[]{status, errorMsg};
            }

            Preparation record = queryPreparationStatus(uid);
            if (record == null) {
                return new String[]{"Unknown", "record not found"};
            }

            status = record.status;
            if ("Created".equalsIgnoreCase(status)) {
                continue;
            }
            if ("Error".equalsIgnoreCase(status)) {
                errorMsg = record.error;
            }
            return new String[]{status, errorMsg};
        }

        return new String[]{status, "timeout"};
    }

    /**
     * Query preparation status by uid. Mirrors Go's queryPreparationStatus.
     */
    @SuppressWarnings("unchecked")
    private Preparation queryPreparationStatus(String uid) {
        try {
            BashUtil.ExecResult statusResult = BashUtil.execScript(bladeBinPath, "status " + uid);
            if (!statusResult.isSuccess()) {
                logger.warn("[chaosblade] query preparation status failed: {}", statusResult.getErrorMsg());
                return null;
            }
            Response resp = parseResult(statusResult.getOutput());
            if (resp == null || resp.getResult() == null) {
                return null;
            }
            if (resp.getResult() instanceof Map) {
                Map<String, Object> fields = (Map<String, Object>) resp.getResult();
                Preparation record = new Preparation();
                record.uid = uid;
                Object pt = fields.get("ProgramType");
                record.programType = pt != null ? pt.toString() : "";
                Object st = fields.get("Status");
                record.status = st != null ? st.toString() : "";
                Object er = fields.get("Error");
                record.error = er != null ? er.toString() : "";
                return record;
            }
            return null;
        } catch (Exception e) {
            logger.warn("[chaosblade] queryPreparationStatus error: {}", e.getMessage());
            return null;
        }
    }

    private void reportStatus(String uid, String status, String errorMsg, String apiName) {
        try {
            Uri uri = TransportUriMap.get(apiName);
            if (uri == null) {
                logger.warn("[chaosblade] report uri is null for: {}", apiName);
                return;
            }
            AsyncReportHandler reporter = new AsyncReportHandler(transportClient);
            reporter.reportStatus(uid, status, errorMsg, "", uri);
        } catch (Exception e) {
            logger.warn("[chaosblade] report status failed: {}", e.getMessage());
        }
    }

    private void waitForK8sStatus(String uid) {
        if (uid == null || uid.isEmpty()) return;
        logger.info("[chaosblade] waiting for K8s experiment status, uid: {}", uid);
        try {
            Thread.sleep(500);
            long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                BashUtil.ExecResult qr = BashUtil.execScript(bladeBinPath, "query k8s create " + uid);
                if (qr.isSuccess()) {
                    Response resp = parseResult(qr.getOutput());
                    if (resp != null && resp.isSuccess() && resp.getResult() != null) {
                        logger.info("[chaosblade] K8s experiment status found, uid: {}", uid);
                        return;
                    }
                }
                Thread.sleep(500);
            }
            logger.warn("[chaosblade] timeout waiting for K8s experiment status, uid: {}", uid);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Extract uid from response, trying multiple strategies.
     * Mirrors Go's extractUidFromResponse.
     */
    @SuppressWarnings("unchecked")
    private String extractUidFromResponse(Response response, String rawResult) {
        if (response == null) {
            return extractUidFromRawResult(rawResult);
        }

        // Try Result as String
        Object result = response.getResult();
        if (result instanceof String) {
            String uid = (String) result;
            if (!uid.isEmpty()) return uid;
        }

        // Try Result as Map with "uid" key
        if (result instanceof Map) {
            Object uid = ((Map<String, Object>) result).get("uid");
            if (uid != null) {
                String uidStr = uid.toString();
                if (!uidStr.isEmpty()) return uidStr;
            }
        }

        // Fallback to raw result
        return extractUidFromRawResult(rawResult);
    }

    /**
     * Extract uid from raw JSON result string.
     * Mirrors Go's extractUidFromRawResult.
     */
    @SuppressWarnings("unchecked")
    private String extractUidFromRawResult(String rawResult) {
        if (rawResult == null || rawResult.isEmpty()) return "";
        try {
            Map<String, Object> resultMap = objectMapper.readValue(rawResult, Map.class);
            // Check result.uid
            Object resultObj = resultMap.get("result");
            if (resultObj instanceof Map) {
                Object uid = ((Map<String, Object>) resultObj).get("uid");
                if (uid != null) return uid.toString();
            }
            if (resultObj instanceof String) {
                return (String) resultObj;
            }
            // Check top-level uid
            Object uid = resultMap.get("uid");
            if (uid != null) return uid.toString();
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    /**
     * Simple uid extraction from response result.
     */
    @SuppressWarnings("unchecked")
    private String extractUid(Response response) {
        if (response == null || response.getResult() == null) return "";
        Object result = response.getResult();
        if (result instanceof String) return (String) result;
        if (result instanceof Map) {
            Object uid = ((Map<String, Object>) result).get("uid");
            return uid != null ? uid.toString() : "";
        }
        return "";
    }

    private static boolean isCreateOrPrepare(String cmd) {
        return CREATE_OPS.contains(cmd) || PREPARE_OPS.contains(cmd);
    }

    private static boolean isDestroyOrRevoke(String cmd) {
        return DESTROY_OPS.contains(cmd) || REVOKE_OPS.contains(cmd);
    }

    private static boolean isK8sCreateCmd(String cmd, String command) {
        if (!CREATE_OPS.contains(command)) return false;
        String[] fields = cmd.trim().split("\\s+");
        return fields.length >= 2 && "k8s".equalsIgnoreCase(fields[1]);
    }

    private static boolean isAsyncCreate(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        if (parts.length == 0 || !CREATE_OPS.contains(parts[0])) return false;
        for (String p : parts) {
            if (p.startsWith("--") && p.length() > 2 && "async".equals(p.substring(2))) return true;
        }
        return false;
    }

    /** Preparation record from blade status query. */
    private static class Preparation {
        String uid;
        String programType;
        String status;
        String error;
    }
}
