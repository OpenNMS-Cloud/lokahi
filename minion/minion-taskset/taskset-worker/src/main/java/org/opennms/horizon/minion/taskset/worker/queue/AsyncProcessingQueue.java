package org.opennms.horizon.minion.taskset.worker.queue;

import java.util.function.Consumer;

public interface AsyncProcessingQueue<T> {
    /**
     * Set the consumer that processes messages out of the queue.
     *
     * @param consumer
     */
    void setConsumer(Consumer<T> consumer);

    /**
     * Send a message to the queue for downstream processing.
     *
     * @param msg
     */
    void asyncSend(T msg);
}
