package org.opennms.horizon.minion.scheduler;

import java.util.concurrent.TimeUnit;

public interface OpennmsScheduler {
    void scheduleTaskOnCron(String taskId, String cronExpression, Runnable operation);
    void schedulePeriodically(String taskId, long period, TimeUnit unit, Runnable operation);
    void scheduleOnce(String taskId, long period, TimeUnit unit, Runnable operation);
    void cancelTask(String taskId);
    void shutdown();
}
