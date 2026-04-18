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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Error code constants and message templates.
 * Mirrors Go's transport/response.go error codes.
 */
public final class ErrorCodes {

    public static final int OK = 200;

    public static final int INVALID_TIMESTAMP = 401;
    public static final int FORBIDDEN = 403;
    public static final int HANDLER_NOT_FOUND = 404;
    public static final int TOKEN_NOT_FOUND = 405;
    public static final int PARAMETER_EMPTY = 406;
    public static final int PARAMETER_LESS = 407;
    public static final int PARAMETER_TYPE_ERROR = 408;

    public static final int SERVER_ERROR = 500;
    public static final int SERVICE_NOT_OPENED = 501;
    public static final int SERVICE_NOT_AUTHORIZED = 502;
    public static final int ENCODE_ERROR = 503;
    public static final int SERVICE_SWITCH_ERROR = 504;
    public static final int HANDLER_CLOSED = 505;
    public static final int SERVICE_NOT_SUPPORT = 506;
    public static final int CTL_FILE_NOT_FOUND = 507;
    public static final int CTL_EXEC_FAILED = 508;

    public static final int CHAOSBLADE_FILE_NOT_FOUND = 600;
    public static final int RESULT_UNMARSHAL_FAILED = 601;
    public static final int HELM3_EXEC_ERROR = 602;

    public static final Map<Integer, String> ERRORS;

    static {
        Map<Integer, String> m = new HashMap<>();
        m.put(OK, "success");

        m.put(INVALID_TIMESTAMP, "invalid timestamp");
        m.put(FORBIDDEN, "forbidden, err: %s");
        m.put(HANDLER_NOT_FOUND, "request handler not found");
        m.put(TOKEN_NOT_FOUND, "access token not found");
        m.put(PARAMETER_EMPTY, "`%s`: parameter is empty");
        m.put(PARAMETER_LESS, "`%s`: parameter less");
        m.put(PARAMETER_TYPE_ERROR, "`%s` parameter data error");

        m.put(SERVER_ERROR, "server error, err: %s");
        m.put(SERVICE_NOT_OPENED, "chaos service not opened");
        m.put(SERVICE_NOT_AUTHORIZED, "chaos service not authorized");
        m.put(ENCODE_ERROR, "encode error, err: %s");
        m.put(SERVICE_SWITCH_ERROR, "service switch error, err: %s");
        m.put(HANDLER_CLOSED, "service handler closed");
        m.put(SERVICE_NOT_SUPPORT, "service not support: %s");
        m.put(CTL_FILE_NOT_FOUND, "`%s`: ctl file not found");
        m.put(CTL_EXEC_FAILED, "exec ctl file failed: %s");

        m.put(CHAOSBLADE_FILE_NOT_FOUND, "chaosblade file not found");
        m.put(RESULT_UNMARSHAL_FAILED, "`%s`: exec result unmarshal failed, err: %s");
        m.put(HELM3_EXEC_ERROR, "helm3 exec error, err: %s");

        ERRORS = Collections.unmodifiableMap(m);
    }

    private ErrorCodes() {
    }
}
