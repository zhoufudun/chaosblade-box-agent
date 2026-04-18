package com.alibaba.chaosblade.box.agent.web;

import com.alibaba.chaosblade.box.agent.pkg.BashUtil;
import com.alibaba.chaosblade.box.agent.pkg.FileUtil;
import com.alibaba.chaosblade.box.agent.transport.ErrorCodes;
import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Handles agent uninstall requests.
 * Mirrors Go's web/handler/uninstallAgent.go.
 */
public class UninstallHandler implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(UninstallHandler.class);

    private final Supplier<String> ctlPathSupplier;

    public UninstallHandler(Supplier<String> ctlPathSupplier) {
        this.ctlPathSupplier = ctlPathSupplier;
    }

    @Override
    public Response handle(Request request) {
        logger.info("Receive server uninstall agent request");

        String ctlPath = ctlPathSupplier.get();
        if (ctlPath == null || ctlPath.isEmpty() || !FileUtil.isExist(ctlPath)) {
            logger.warn("ctl file not found: {}", ctlPath);
            return Response.returnFail(ErrorCodes.CTL_FILE_NOT_FOUND, ctlPath);
        }

        try {
            BashUtil.ExecResult result = BashUtil.execScript(ctlPath, "uninstall");
            if (!result.isSuccess()) {
                logger.warn("exec ctl file failed: {}", result.getErrorMsg());
                return Response.returnFail(ErrorCodes.CTL_EXEC_FAILED, result.getErrorMsg());
            }
        } catch (Exception e) {
            logger.warn("exec ctl file failed: {}", e.getMessage());
            return Response.returnFail(ErrorCodes.CTL_EXEC_FAILED, e.getMessage());
        }

        return Response.returnSuccess();
    }
}
