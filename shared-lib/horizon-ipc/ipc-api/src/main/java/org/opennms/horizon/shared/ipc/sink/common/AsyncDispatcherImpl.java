/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package org.opennms.horizon.shared.ipc.sink.common;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.SendQueue;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;
import org.opennms.horizon.shared.logging.LogPreservingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.Message;
import com.swrve.ratelimitedlogger.RateLimitedLog;

public class AsyncDispatcherImpl<W, S extends Message, T extends Message> implements AsyncDispatcher<S> {

    public static final String WHAT_IS_DEFAULT_INSTANCE_ID = "OpenNMS";

    private static final Logger LOG = LoggerFactory.getLogger(AsyncDispatcherImpl.class);

    private final SendQueue<T> sendQueue;
    private final MessageDispatcher<S, T> messageDispatcher;
    private final Consumer<byte[]> sender;
    private final AsyncPolicy asyncPolicy;
    private final Counter droppedCounter;

    private final AtomicLong missedFutures = new AtomicLong(0);
    private final AtomicInteger activeDispatchers = new AtomicInteger(0);
    
    private final RateLimitedLog RATE_LIMITED_LOGGER = RateLimitedLog
            .withRateLimit(LOG)
            .maxRate(5).every(Duration.ofSeconds(30))
            .build();

    private final ExecutorService executor;

    public AsyncDispatcherImpl(final DispatcherState<W, S, T> state,
                               final SendQueueFactory sendQueueFactory,
                               final Consumer<byte[]> sender) {
        this.sendQueue = sendQueueFactory.createQueue(state.getModule());

        this.messageDispatcher = AbstractMessageDispatcherFactory.createMessageDispatcher(state, this.sendQueue::enqueue);

        this.sender = Objects.requireNonNull(sender);

        this.asyncPolicy = state.getModule().getAsyncPolicy();

        state.getMetrics().register(MetricRegistry.name(state.getModule().getId(), "queue-size"),
                (Gauge<Integer>) activeDispatchers::get);

        droppedCounter = state.getMetrics().counter(MetricRegistry.name(state.getModule().getId(), "dropped"));

        executor = Executors.newFixedThreadPool(state.getModule().getAsyncPolicy().getNumThreads(),
                new LogPreservingThreadFactory(WHAT_IS_DEFAULT_INSTANCE_ID + ".Sink.AsyncDispatcher." +
                        state.getModule().getId(), Integer.MAX_VALUE));

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
    public void send(S message) {
        if (!this.asyncPolicy.isBlockWhenFull() && this.sendQueue.isFull()) {
            this.droppedCounter.inc();
            throw new RuntimeException("Dispatch queue full");
        }

        this.messageDispatcher.dispatch(message);
    }

    @Override
    public void close() throws Exception {
        this.messageDispatcher.close();
        this.executor.shutdown();
        this.sendQueue.close();
    }
}
