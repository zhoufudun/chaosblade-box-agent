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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transport response object.
 * Mirrors Go's transport/response.go Response struct.
 *
 * Uses uppercase @JsonProperty to match Go's default JSON marshaling (field names: Code, Success, Error, Result).
 * Uses @JsonAlias for lowercase variants so Jackson can also deserialize lowercase JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {

    @JsonProperty("Code")
    @JsonAlias("code")
    private int code;

    @JsonProperty("Success")
    @JsonAlias("success")
    private boolean success;

    @JsonProperty("Error")
    @JsonAlias({"error", "Err"})
    private String error;

    @JsonProperty("Result")
    @JsonAlias("result")
    private Object result;

    public Response() {
    }

    public Response(int code, boolean success, String error, Object result) {
        this.code = code;
        this.success = success;
        this.error = error;
        this.result = result;
    }

    /**
     * Create a failure response using an error code and optional format args.
     */
    public static Response returnFail(int errCode, Object... args) {
        String template = ErrorCodes.ERRORS.get(errCode);
        String errorMsg;
        if (template != null && args != null && args.length > 0) {
            errorMsg = String.format(template, args);
        } else {
            errorMsg = template != null ? template : "unknown error";
        }
        return new Response(errCode, false, errorMsg, null);
    }

    /**
     * Create a success response.
     */
    public static Response returnSuccess() {
        return new Response(ErrorCodes.OK, true, "", "success");
    }

    /**
     * Create a success response with a result payload.
     */
    public static Response returnSuccessWithResult(Object result) {
        return new Response(ErrorCodes.OK, true, "", result);
    }

    // --- Getters and Setters ---

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
