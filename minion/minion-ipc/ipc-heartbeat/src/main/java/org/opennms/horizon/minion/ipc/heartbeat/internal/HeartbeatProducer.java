/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.minion.ipc.heartbeat.internal;


import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.ipc.sink.api.SyncDispatcher;

import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@RequiredArgsConstructor
public class HeartbeatProducer {

    private static final int PERIOD_MS = 30 * 1000;

    private final IpcIdentity identity;
    private final MessageDispatcherFactory messageDispatcherFactory;

    private SyncDispatcher<HeartbeatMessage> dispatcher;
    private Timer timer = new Timer();


    public void init() {

        dispatcher = messageDispatcherFactory.createSyncDispatcher(new HeartbeatModule());
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    log.info("Sending heartbeat from Minion with id: {}", identity.getId());

                    long millis = System.currentTimeMillis();
                    HeartbeatMessage heartbeatMessage = HeartbeatMessage.newBuilder()
                        .setIdentity(Identity.newBuilder().setSystemId(identity.getId()))
                        .setTimestamp(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)))
                        .build();
                    dispatcher.send(heartbeatMessage);
                } catch (Throwable t) {
                    if (log.isDebugEnabled()) {
                        log.debug("An error occurred while sending the heartbeat. Will try again in {} ms", PERIOD_MS, t);
                    } else {
                        log.debug("An error {} occurred while sending the heartbeat. Will try again in {} ms", PERIOD_MS, t.getMessage());
                    }
                }
            }
        }, 0, PERIOD_MS);
    }

    /**
     * Used to cancel the timer when the Blueprint is destroyed.
     */
    public void cancel() {
        try {
            timer.cancel();
            dispatcher.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
