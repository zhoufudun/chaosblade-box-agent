package com.alibaba.chaosblade.box.agent.web;

import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an ApiHandler with interceptor chain validation and JSON serialization.
 * Mirrors Go's web/api/handler.go ServerRequestHandler.
 */
public class ServerRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServerRequestHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ApiHandler handler;

    public ServerRequestHandler(ApiHandler handler) {
        this.handler = handler;
    }

    /**
     * Process a raw JSON request string: deserialize → handle → serialize response.
     * Note: interceptor chain is NOT applied on inbound (server→agent) requests,
     * matching Go agent behavior where ServerRequestHandler calls Handler.Handle directly
     * without interceptor validation. The server does not send ts/sign fields.
     */
    public String handle(String requestJson) {
        try {
            Request request = objectMapper.readValue(requestJson, Request.class);

            Response response = handler.handle(request);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.warn("[ServerRequestHandler] handle error: {}", e.getMessage());
            try {
                return objectMapper.writeValueAsString(Response.returnFail(500, e.getMessage()));
            } catch (Exception ex) {
                return "{\"success\":false,\"error\":\"internal error\"}";
            }
        }
    }
}
