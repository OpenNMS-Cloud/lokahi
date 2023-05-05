/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import java.util.concurrent.atomic.AtomicLong;

import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumerManager;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.logging.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;


public abstract class AbstractMessageConsumerManager implements MessageConsumerManager {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMessageConsumerManager.class);

    public static final String SINK_INITIAL_SLEEP_TIME = "org.opennms.core.ipc.sink.initialSleepTime";

    private final AtomicLong threadCounter = new AtomicLong();
    private final ThreadFactory threadFactory = (runnable) -> new Thread(runnable, "consumer-starter-" + threadCounter.incrementAndGet());

    protected final ExecutorService startupExecutor = Executors.newCachedThreadPool(threadFactory);

    private final Map<SinkModule<? extends Message, ? extends Message>, Set<MessageConsumer<? extends Message, ? extends Message>>> consumersByModule = new ConcurrentHashMap<>();

    protected abstract <S extends Message, T extends Message> void startConsumingForModule(SinkModule<S, T> module);

    protected abstract void stopConsumingForModule(SinkModule<? extends Message, ? extends Message> module);

    public final CompletableFuture<Void> waitForStartup;

    protected AbstractMessageConsumerManager() {
        // By default, do not introduce any delay on startup.  Can use a Timer that simply calls future.complete()
        CompletableFuture<Void> startupFuture = CompletableFuture.completedFuture(null);
        String initialSleepString = System.getProperty(SINK_INITIAL_SLEEP_TIME, "0");
        try {
            int initialSleep = Integer.parseInt(initialSleepString);
            if (initialSleep > 0) {
                // TODO: async timer instead of sleep in runnable?
                startupFuture = CompletableFuture.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(initialSleep);
                        } catch (InterruptedException e) {
                            LOG.warn(e.getMessage(), e);
                        }
                    }
                }, startupExecutor);
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid value for system property {}: {}", SINK_INITIAL_SLEEP_TIME, initialSleepString);
        }
        waitForStartup = startupFuture;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Message, T extends Message> void dispatch(SinkModule<S, T> module, T message) {
        consumersByModule.get(module)
            .forEach(c -> ((MessageConsumer<S, T>) c).handleMessage(message));
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <S extends Message, T extends Message> void registerConsumer(MessageConsumer<S, T> consumer) {
        if (consumer == null) {
            return;
        }

        try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(MessageConsumerManager.LOG_PREFIX)) {
            LOG.info("Registering consumer: {}", consumer);
            final var module = consumer.getModule();
            final int numConsumersBefore = consumersByModule.computeIfAbsent(module, (key) -> Collections.synchronizedSet(new HashSet<>())).size();
            if (!consumersByModule.containsKey(module)) {
                consumersByModule.put(module, new CopyOnWriteArraySet<>());
            }
            consumersByModule.get(module).add(consumer);
            if (numConsumersBefore < 1) {
                waitForStartup.thenRunAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            LOG.info("Starting to consume messages for module: {}", module.getId());
                            startConsumingForModule(module);
                        } catch (Exception e) {
                            LOG.error("Unexpected exception while trying to start consumer for module: {}", module.getId(), e);
                        }
                    }
                }, startupExecutor);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <S extends Message, T extends Message> void unregisterConsumer(MessageConsumer<S, T> consumer) {
        if (consumer == null) {
            return;
        }

        try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(MessageConsumerManager.LOG_PREFIX)) {
            LOG.info("Unregistering consumer: {}", consumer);
            final SinkModule<? extends Message, ? extends Message> module = consumer.getModule();

            final var consumers = this.consumersByModule.get(module);
            if (consumers != null) {
                consumers.remove(consumer);
                if (consumers.isEmpty()) {
                    waitForStartup.thenRunAsync(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LOG.info("Stopping consumption of messages for module: {}", module.getId());
                                stopConsumingForModule(module);
                            } catch (Exception e) {
                                LOG.error("Unexpected exception while trying to stop consumer for module: {}", module.getId(), e);
                            }
                        }
                    });
                }
            }
        }
    }

    public synchronized void unregisterAllConsumers() {
        // Copy the list of consumers before we iterate to avoid concurrent modification exceptions
        final var consumers = consumersByModule.values().stream()
            .flatMap(Collection::stream)
            .toList();
        for (final var consumer : consumers) {
            unregisterConsumer(consumer);
        }
    }

    public static int getNumConsumerThreads(SinkModule<?, ?> module) {
        Objects.requireNonNull(module);
        final int defaultValue = Runtime.getRuntime().availableProcessors() * 2;
        final int configured = module.getNumConsumerThreads();
        if (configured <= 0) {
            LOG.warn("Number of consumer threads for module {} was {}. Value must be > 0. Falling back to {}", module.getId(), configured, defaultValue);
            return defaultValue;
        }
        return configured;
    }
}
