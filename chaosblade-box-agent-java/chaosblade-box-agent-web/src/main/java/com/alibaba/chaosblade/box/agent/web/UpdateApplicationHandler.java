package com.alibaba.chaosblade.box.agent.web;

import com.alibaba.chaosblade.box.agent.pkg.AuthUtil;
import com.alibaba.chaosblade.box.agent.transport.ErrorCodes;
import com.alibaba.chaosblade.box.agent.transport.Request;
import com.alibaba.chaosblade.box.agent.transport.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Updates application instance and group names, writes to .chaos.app file.
 * Mirrors Go's web/handler/updateApplication.go.
 */
public class UpdateApplicationHandler implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateApplicationHandler.class);
    private static final String APP_INSTANCE_KEY = "appInstance";
    private static final String APP_GROUP_KEY = "appGroup";

    private final Consumer<String[]> optionsUpdater;

    /**
     * @param optionsUpdater callback to update [appInstance, appGroup] in the global options
     */
    public UpdateApplicationHandler(Consumer<String[]> optionsUpdater) {
        this.optionsUpdater = optionsUpdater;
    }

    @Override
    public Response handle(Request request) {
        logger.info("Receive server update application request");

        Map<String, String> params = request.getParams();
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        String appInstance = params.get(APP_INSTANCE_KEY);
        String appGroup = params.get(APP_GROUP_KEY);

        // Update in-memory options first (matches Go: options.Opts.ApplicationInstance = appInstance)
        if (optionsUpdater != null) {
            optionsUpdater.accept(new String[]{appInstance, appGroup});
        }

        // Write to .chaos.app file using key=value format (matches Go's RecordApplicationToFile)
        try {
            AuthUtil.recordApplicationToFile(
                    appInstance != null ? appInstance : "",
                    appGroup != null ? appGroup : "",
                    true);
        } catch (Exception e) {
            logger.warn("record application info to local file failed: {}", e.getMessage());
            return Response.returnFail(ErrorCodes.SERVER_ERROR, "record application info to local file failed");
        }

        return Response.returnSuccess();
    }
}
