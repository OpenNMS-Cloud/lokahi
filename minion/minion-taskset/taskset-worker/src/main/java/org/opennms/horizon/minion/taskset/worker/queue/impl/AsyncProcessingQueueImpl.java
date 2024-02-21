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
package org.opennms.horizon.minion.taskset.worker.queue.impl;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.Setter;
import org.opennms.horizon.minion.taskset.worker.queue.AsyncProcessingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory asynchronous queue implementation.  Requires a ThreadPoolExecutor and Consumer to be injected to process
 * the messages.
 *
 * The consumer is called on dispatch.
 *
 * @param <T>
 */
@Deprecated // may be replaced by ipc api or even a camel seda queue
public class AsyncProcessingQueueImpl<T> implements AsyncProcessingQueue<T> {

    public static final int DEFAULT_MAX_QUEUE_SIZE = 1_000_000;
    public static final int THREAD_STARVATION_LIMIT = 1_000;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(AsyncProcessingQueueImpl.class);

    private Logger log = DEFAULT_LOGGER;

    @Setter
    private ThreadPoolExecutor executor;

    @Setter
    private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;

    private Consumer<T> consumer;

    private boolean dispatchActive;

    private LinkedBlockingDeque<T> messageStore;

    private final Object lock = new Object();

    private AtomicLong totalEnqueued = new AtomicLong(0);
    private AtomicLong totalDequeued = new AtomicLong(0);
    private boolean noConsumerWarningGiven = false;

    // ========================================
    // Lifecycle
    // ----------------------------------------

    public void init() {
        messageStore = new LinkedBlockingDeque<>(maxQueueSize);
    }

    // ========================================
    // AsyncProcessingQueue interface
    // ----------------------------------------

    @Override
    public void setConsumer(Consumer<T> consumer) {
        this.consumer = consumer;
        triggerDispatch();
    }

    @Override
    public void asyncSend(T msg) {
        messageStore.add(msg);
        totalEnqueued.incrementAndGet();
        triggerDispatch();
    }

    // ========================================
    // Internal
    // ----------------------------------------

    private void triggerDispatch() {
        try {
            synchronized (lock) {
                if (!dispatchActive) {
                    dispatchActive = true;
                    this.executor.execute(this::executeDispatch);
                }
            }
        } catch (RejectedExecutionException reExc) {
            log.warn("Execution of dispatch rejected - may need to increase thread pool size", reExc);
        }
    }

    private void executeDispatch() {
        long numDispatched = 0;

        // Make sure the consumer is ready and just skip the dispatch if not.
        if (!checkConsumerReady()) {
            return;
        }

        // Make sure the consumer is ready
        synchronized (lock) {
            if (consumer == null) {
                if (!noConsumerWarningGiven) {
                    noConsumerWarningGiven = true;
                    log.warn(
                            "Have dispatch request when no consumer yet ready on queue - make sure setConsumer() is called before send()");
                }

                return;
            }
        }

        /**
         * Loop until the dispatch is ready to stop, when the queue is empty.  Note the starvation check at the end
         *  of the loop.
         */
        while (!dispatchReadyToStop()) {
            T msg = messageStore.remove();

            totalDequeued.incrementAndGet();

            try {
                consumer.accept(msg);
            } catch (Exception exc) {
                log.warn("MESSAGE CONSUMER propagated exception to dispatch", exc);
            }

            // Prevent starvation of the executing thread on the thread pool if the queue contains a very large backlog.
            numDispatched++;
            if (numDispatched > THREAD_STARVATION_LIMIT) {
                yieldExecutorThread();
                return;
            }
        }
    }

    private boolean checkConsumerReady() {
        synchronized (lock) {
            if (consumer == null) {
                if (!noConsumerWarningGiven) {
                    noConsumerWarningGiven = true;
                    log.warn(
                            "Have dispatch request when no consumer yet ready on queue - make sure setConsumer() is called before send()");
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Atomically check whether the dispatcher is ready to stop and mark it as inactive if so.
     *
     * @return
     */
    private boolean dispatchReadyToStop() {
        boolean result = false;

        /**
         * In the critical section, check if the store is empty, and reset dispatchActive if so.  This eliminates the
         *  race condition on termination of the dispatch
         */
        synchronized (lock) {
            if (messageStore.isEmpty()) {
                result = true;
                dispatchActive = false;
            }
        }

        return result;
    }

    /**
     * Yield the thread by stopping the dispatch for now and requesting another execution by the executor.  This reduces
     *  the impact that a queue with a large backlog will have on the thread pool, which ideally is shared for other
     *  processing.
     */
    private void yieldExecutorThread() {
        synchronized (lock) {
            dispatchActive = false;
            executor.execute(this::executeDispatch);
        }
    }
}
