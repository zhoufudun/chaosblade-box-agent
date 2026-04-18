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

package com.alibaba.chaosblade.box.agent.monitor;

import com.alibaba.chaosblade.box.agent.conn.HBSnapshot;
import com.alibaba.chaosblade.box.agent.conn.HeartbeatHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Default health checker that monitors heartbeat snapshots.
 * Reverse-iterates the HBSnapshotList, counts consecutive failures/successes,
 * and triggers stop/start events based on thresholds.
 *
 * Mirrors Go's monitor/checker.go defaultChecker.
 */
public class DefaultChecker implements Checker {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChecker.class);

    static final int HB_STOP_THRESHOLD = 12;
    static final int HB_START_THRESHOLD = 3;

    /** Tracks whether a stop event has already been triggered. */
    private volatile boolean hbAlreadyStopped = false;

    @Override
    public MonitorAction check() {
        MonitorAction action = new MonitorAction();
        checkHeartbeat(action);

        if (!action.isNeedStop() && !action.isNeedExit() && action.isNeedStart()) {
            hbAlreadyStopped = false;
        }

        return action;
    }

    /**
     * Exposed for testing: check if the monitor considers the agent stopped.
     */
    boolean isHbAlreadyStopped() {
        return hbAlreadyStopped;
    }

    private void checkHeartbeat(MonitorAction action) {
        final int[] failCount = {0};
        final int[] succCount = {0};
        final List<HBSnapshot> walkerList = new ArrayList<>();
        final MonitorAction[] actionRef = {action};

        HeartbeatHandler.HB_SNAPSHOT_LIST.foreachReverse(snapshot -> {
            walkerList.add(snapshot);

            if (!snapshot.isSuccess()) {
                failCount[0]++;
                succCount[0] = 0;
            } else {
                succCount[0]++;
                failCount[0] = 0;
            }

            // Consecutive failures reached stop threshold
            if (failCount[0] == HB_STOP_THRESHOLD && !hbAlreadyStopped) {
                hbAlreadyStopped = true;
                actionRef[0].recover();
                actionRef[0].setNeedStop(true);
                actionRef[0].setReason("stop because of heartbeat");
                printWalkerList(actionRef[0].getReason(), walkerList);
                throw new RuntimeException("nolog");
            }

            if (failCount[0] == HB_STOP_THRESHOLD && hbAlreadyStopped) {
                throw new RuntimeException("nolog");
            }

            // Consecutive successes reached start threshold while stopped
            if (succCount[0] == HB_START_THRESHOLD && hbAlreadyStopped) {
                actionRef[0].recover();
                actionRef[0].setNeedStart(true);
                printWalkerList("can start because of heartbeat", walkerList);
                throw new RuntimeException("nolog");
            }

            if (succCount[0] == HB_START_THRESHOLD && !hbAlreadyStopped) {
                throw new RuntimeException("nolog");
            }
        }, true);
    }

    private void printWalkerList(String info, List<HBSnapshot> walkerList) {
        StringBuilder buf = new StringBuilder();
        buf.append(info).append(", walker list is : ");
        for (HBSnapshot s : walkerList) {
            buf.append(s.isSuccess() ? "OK" : "FAIL").append("|");
        }
        logger.warn(buf.toString());
    }
}
