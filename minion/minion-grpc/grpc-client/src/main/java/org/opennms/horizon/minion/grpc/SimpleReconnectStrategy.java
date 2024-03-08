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
package org.opennms.horizon.minion.grpc;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleReconnectStrategy implements Runnable, ReconnectStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleReconnectStrategy.class);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ManagedChannel channel;
    private final Runnable onConnect;
    private final Runnable onDisconnect;
    private ScheduledFuture<?> reconnectTask;

    public SimpleReconnectStrategy(ManagedChannel channel, Runnable onConnect, Runnable onDisconnect) {
        this.channel = channel;
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public synchronized void activate() {
        if (reconnectTask != null) {
            LOG.trace("Ignoring activate request. One is already in progress.");
            return;
        }
        onDisconnect.run();
        reconnectTask = executor.scheduleAtFixedRate(this, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        reconnectTask.cancel(true);
        executor.shutdownNow();
    }

    @Override
    public void run() {
        ConnectivityState state = channel.getState(true);

        if (state == ConnectivityState.READY) {
            // The onConnect callback may block, so we leave this out of any critical section
            onConnect.run();
            synchronized (this) {
                // After successfully triggering onConnect, cancel future executions
                if (reconnectTask != null) {
                    reconnectTask.cancel(false);
                    reconnectTask = null;
                }
            }
        } else {
            LOG.info("Channel is in currently in state: {}. Waiting for it to be READY.", state);
        }
    }
}
