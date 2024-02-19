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

import com.google.protobuf.Message;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

public abstract class AbstractMessageConsumerManager implements MessageConsumerManager {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMessageConsumerManager.class);

    public static final String SINK_INITIAL_SLEEP_TIME = "org.opennms.core.ipc.sink.initialSleepTime";

    private final AtomicLong threadCounter = new AtomicLong();
    private final ThreadFactory threadFactory =
            (runnable) -> new Thread(runnable, "consumer-starter-" + threadCounter.incrementAndGet());

    protected final ExecutorService startupExecutor = Executors.newCachedThreadPool(threadFactory);

    private final Map<
                    SinkModule<? extends Message, ? extends Message>,
                    MessageConsumer<? extends Message, ? extends Message>>
            consumerByModule = new ConcurrentHashMap<>();

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
                startupFuture = CompletableFuture.runAsync(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(initialSleep);
                                } catch (InterruptedException e) {
                                    LOG.warn(e.getMessage(), e);
                                }
                            }
                        },
                        startupExecutor);
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid value for system property {}: {}", SINK_INITIAL_SLEEP_TIME, initialSleepString);
        }
        waitForStartup = startupFuture;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Message, T extends Message> void dispatch(SinkModule<S, T> module, T message) {
        final var consumer = consumerByModule.get(module);
        if (consumer != null) {
            ((MessageConsumer<S, T>) consumer).handleMessage(message);
        }
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
            if (consumerByModule.containsKey(module)) {
                throw new IllegalStateException("Module already registered: " + module.getId());
            }

            consumerByModule.put(module, consumer);

            waitForStartup.thenRunAsync(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LOG.info("Starting to consume messages for module: {}", module.getId());
                                startConsumingForModule(module);
                            } catch (Exception e) {
                                LOG.error(
                                        "Unexpected exception while trying to start consumer for module: {}",
                                        module.getId(),
                                        e);
                            }
                        }
                    },
                    startupExecutor);
        }
    }

    public static int getNumConsumerThreads(SinkModule<?, ?> module) {
        Objects.requireNonNull(module);
        final int defaultValue = Runtime.getRuntime().availableProcessors() * 2;
        final int configured = module.getNumConsumerThreads();
        if (configured <= 0) {
            LOG.warn(
                    "Number of consumer threads for module {} was {}. Value must be > 0. Falling back to {}",
                    module.getId(),
                    configured,
                    defaultValue);
            return defaultValue;
        }
        return configured;
    }
}
