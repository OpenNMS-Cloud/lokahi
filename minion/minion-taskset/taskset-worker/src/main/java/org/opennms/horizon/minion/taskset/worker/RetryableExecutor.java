package org.opennms.horizon.minion.taskset.worker;

import com.google.protobuf.Any;

public interface RetryableExecutor {
    /**
     * Initialize the executor for the workflow.
     *
     * @param handleRetryNeeded callback listening for disconnects in order to schedule reconnect attempts.  Only call
     *                         after a successful attempt() call.
     */
    void init(Runnable handleRetryNeeded);

    /**
     * Attempt the executor.  After success, needs to schedule a retry must be triggered by calling the disconnect
     * handler provided at init time.
     *
     * @throws Exception indicate failure of the attempt; another attempt is automatically scheduled.
     */
    void attempt(Any configuration) throws Exception;

    /**
     * Cancel the executor on shutdown of the workflow.
     */
    void cancel();
}
