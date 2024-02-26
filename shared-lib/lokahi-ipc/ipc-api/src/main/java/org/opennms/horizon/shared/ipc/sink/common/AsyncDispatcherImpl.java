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
package org.opennms.horizon.shared.ipc.sink.common;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.Message;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.SendQueue;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;
import org.opennms.horizon.shared.logging.LogPreservingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncDispatcherImpl<W, S extends Message, T extends Message> implements AsyncDispatcher<S> {

    public static final String WHAT_IS_DEFAULT_INSTANCE_ID = "OpenNMS";

    private static final Logger LOG = LoggerFactory.getLogger(AsyncDispatcherImpl.class);

    private final SendQueue sendQueue;
    private final MessageDispatcher<S, T> messageDispatcher;
    private final Consumer<byte[]> sender;
    private final AsyncPolicy asyncPolicy;

    private final AtomicInteger activeDispatchers = new AtomicInteger(0);

    private final RateLimitedLog RATE_LIMITED_LOGGER = RateLimitedLog.withRateLimit(LOG)
            .maxRate(5)
            .every(Duration.ofSeconds(30))
            .build();

    private final ExecutorService executor;

    public AsyncDispatcherImpl(
            final DispatcherState<W, S, T> state,
            final SendQueueFactory sendQueueFactory,
            final Consumer<byte[]> sender)
            throws IOException {
        this.sendQueue = sendQueueFactory.createQueue(state.getModule().getId());

        this.messageDispatcher =
                AbstractMessageDispatcherFactory.createMessageDispatcher(state, this.sendQueue::enqueue);

        this.sender = Objects.requireNonNull(sender);

        this.asyncPolicy = state.getModule().getAsyncPolicy();

        state.getMetrics().register(MetricRegistry.name(state.getModule().getId(), "queue-size"), (Gauge<Integer>)
                activeDispatchers::get);

        executor = Executors.newFixedThreadPool(
                state.getModule().getAsyncPolicy().getNumThreads(),
                new LogPreservingThreadFactory(
                        WHAT_IS_DEFAULT_INSTANCE_ID + ".Sink.AsyncDispatcher."
                                + state.getModule().getId(),
                        Integer.MAX_VALUE));

        startDrainingQueue();
    }

    private void dispatchFromQueue() {
        while (true) {
            try {
                LOG.trace("Asking send queue for the next entry...");
                final var message = this.sendQueue.dequeue();

                LOG.trace("Received message entry from dispatch queue");
                activeDispatchers.incrementAndGet();

                LOG.trace("Sending message {}", message);
                this.sender.accept(message);

                LOG.trace("Successfully sent message {}", message);

                activeDispatchers.decrementAndGet();
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                RATE_LIMITED_LOGGER.warn("Encountered exception while taking from dispatch queue", e);
            }
        }
    }

    private void startDrainingQueue() {
        for (int i = 0; i < this.asyncPolicy.getNumThreads(); i++) {
            executor.execute(this::dispatchFromQueue);
        }
    }

    @Override
    public void send(S message) throws InterruptedException {
        this.messageDispatcher.dispatch(message);
    }

    @Override
    public void close() throws Exception {
        this.messageDispatcher.close();
        this.executor.shutdown();
        this.sendQueue.close();
    }
}
