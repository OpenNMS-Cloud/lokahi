package org.opennms.horizon.minion.scheduler.impl;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledThreadPoolExecutorFactory {
    private AtomicLong threadCounter = new AtomicLong(0);

    public ScheduledThreadPoolExecutor create(int numThread, String threadNamePrefix) {
        ThreadFactory threadFactory = runnable -> {
            Thread result = new Thread(runnable);
            result.setDaemon(true);
            result.setName(formatThreadName(threadNamePrefix));

            return result;
        };

        return new ScheduledThreadPoolExecutor(numThread, threadFactory);
    }

    private String formatThreadName(String prefix) {
        return prefix + threadCounter.getAndIncrement();
    }
}
