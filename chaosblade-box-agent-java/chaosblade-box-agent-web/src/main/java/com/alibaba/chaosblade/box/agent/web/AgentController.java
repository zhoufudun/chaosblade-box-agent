package com.alibaba.chaosblade.box.agent.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Boot REST controller that dispatches requests to registered handlers.
 * Mirrors Go's web/server/http.go HTTP server with handler routing.
 */
@RestController
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final Map<String, ServerRequestHandler> handlers = new ConcurrentHashMap<>();

    public void registerHandler(String name, ServerRequestHandler handler) {
        handlers.put(name, handler);
        logger.info("[AgentController] registered handler: {}", name);
    }

    /**
     * Handle POST requests. The request body JSON is passed in the "body" form parameter.
     */
    @PostMapping(value = "/{handlerName}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleRequest(
            @PathVariable String handlerName,
            @RequestParam("body") String body) {

        ServerRequestHandler handler = handlers.get(handlerName);
        if (handler == null) {
            logger.warn("[AgentController] handler not found: {}", handlerName);
            return ResponseEntity.notFound().build();
        }

        logger.debug("[AgentController] dispatching to handler: {}", handlerName);
        String response = handler.handle(body);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Also support JSON body directly for flexibility.
     */
    @PostMapping(value = "/{handlerName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleJsonRequest(
            @PathVariable String handlerName,
            @RequestBody String body) {

        ServerRequestHandler handler = handlers.get(handlerName);
        if (handler == null) {
            logger.warn("[AgentController] handler not found: {}", handlerName);
            return ResponseEntity.notFound().build();
        }

        String response = handler.handle(body);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
