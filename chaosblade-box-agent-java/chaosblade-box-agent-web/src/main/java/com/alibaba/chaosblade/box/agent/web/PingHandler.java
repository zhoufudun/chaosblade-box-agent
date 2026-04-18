package com.alibaba.chaosblade.box.agent.web;

import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingHandler implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(PingHandler.class);

    @Override
    public Response handle(Request request) {
        logger.info("Receive server ping request");
        return Response.returnSuccess();
    }
}
