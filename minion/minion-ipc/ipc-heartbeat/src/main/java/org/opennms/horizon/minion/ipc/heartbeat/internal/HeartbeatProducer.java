/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.ipc.heartbeat.internal;

import com.google.protobuf.Timestamp;
import java.util.Timer;
import java.util.TimerTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.ipc.sink.api.SyncDispatcher;

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
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            log.info("Sending heartbeat from Minion with id: {}", identity.getId());

                            long millis = System.currentTimeMillis();
                            HeartbeatMessage heartbeatMessage = HeartbeatMessage.newBuilder()
                                    .setIdentity(Identity.newBuilder().setSystemId(identity.getId()))
                                    .setTimestamp(Timestamp.newBuilder()
                                            .setSeconds(millis / 1000)
                                            .setNanos((int) ((millis % 1000) * 1000000)))
                                    .build();
                            dispatcher.send(heartbeatMessage);
                        } catch (Throwable t) {
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "An error occurred while sending the heartbeat. Will try again in {} ms",
                                        PERIOD_MS,
                                        t);
                            } else {
                                log.debug(
                                        "An error {} occurred while sending the heartbeat. Will try again in {} ms",
                                        PERIOD_MS,
                                        t.getMessage());
                            }
                        }
                    }
                },
                0,
                PERIOD_MS);
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
