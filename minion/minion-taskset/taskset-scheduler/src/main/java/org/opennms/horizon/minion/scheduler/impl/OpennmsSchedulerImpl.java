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
package org.opennms.horizon.minion.scheduler.impl;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.minion.scheduler.OpennmsScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class OpennmsSchedulerImpl implements OpennmsScheduler {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(OpennmsSchedulerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    // Prepare the parser.  Note that the "CRON DEFINITION" specifies WHICH FIELDS (and variations on field inputs) are
    // supported.
    // For now, just use the QUARTZ setting
    private CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    @Getter
    @Setter
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    private Map<String, RunningTaskInfo> scheduledTasks = new ConcurrentHashMap<>();

    // ========================================
    // Lifecycle Management
    // ----------------------------------------

    public void shutdown() {
        log.info("Shutting down scheduler");
        scheduledThreadPoolExecutor.shutdownNow();
    }

    // ========================================
    // Operations
    // ----------------------------------------

    @Override
    public void scheduleTaskOnCron(String taskId, String cronExpression, Runnable operation) {
        Cron cron = cronParser.parse(cronExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);

        RecurringCronRunner runner = new RecurringCronRunner(executionTime, operation);

        // Remember the task before executing to avoid a possible race condition on reschedule
        RunningTaskInfo taskInfo = new RunningTaskInfo(runner);
        RunningTaskInfo old = scheduledTasks.put(taskId, taskInfo);
        if (old != null) {
            log.debug("replacing existing operation for task: task-id={}", taskId);
            unscheduleTask(old);
        }

        // Trigger the next run.  After that, it will reschedule itself.
        Future<?> future = scheduleNextRun(executionTime, runner);
        safeSetTaskInfoFuture(taskInfo, future);
    }

    @Override
    public void schedulePeriodically(String taskId, long period, TimeUnit unit, Runnable operation) {
        RunningTaskInfo taskInfo = new RunningTaskInfo(operation);
        RunningTaskInfo old = scheduledTasks.put(taskId, taskInfo);
        if (old != null) {
            log.debug("replacing existing operation for task: task-id={}", taskId);
            unscheduleTask(old);
        }

        Future<?> future = scheduledThreadPoolExecutor.scheduleAtFixedRate(operation, 0, period, unit);
        safeSetTaskInfoFuture(taskInfo, future);
    }

    @Override
    public void scheduleOnce(String taskId, long period, TimeUnit unit, Runnable operation) {
        RunningTaskInfo taskInfo = new RunningTaskInfo(operation);
        RunningTaskInfo old = scheduledTasks.put(taskId, taskInfo);
        if (old != null) {
            log.debug("replacing existing operation for task: task-id={}", taskId);
            unscheduleTask(old);
        }

        // Use a wrapper to remove the task from scheduledTasks once it completes.
        OneShotTaskWrapper wrapper = new OneShotTaskWrapper(taskId, operation);
        Future<?> future = scheduledThreadPoolExecutor.schedule(wrapper, period, unit);

        // Remember the future in order to properly cancel it later, as-needed
        safeSetTaskInfoFuture(taskInfo, future);
    }

    @Override
    public void cancelTask(String taskId) {
        RunningTaskInfo taskInfo = scheduledTasks.remove(taskId);

        if (taskInfo != null) {
            if (taskInfo.getRunnable() instanceof RecurringCronRunner) {
                // Tell the recurring runner to stop too
                ((RecurringCronRunner) taskInfo.getRunnable()).shutdown();
            }

            unscheduleTask(taskInfo);
        }
    }

    // ========================================
    // Internal Operations
    // ----------------------------------------

    private Future<?> scheduleNextRun(ExecutionTime executionTime, Runnable operation) {
        ZonedDateTime now = ZonedDateTime.now();

        // Get an Optional, but the contract says it will never be null...
        Optional<Duration> durationOpt = executionTime.timeToNextExecution(now);
        Duration duration = durationOpt.get();

        Future<?> result = scheduledThreadPoolExecutor.schedule(operation, duration.toMillis(), TimeUnit.MILLISECONDS);

        return result;
    }

    private void unscheduleTask(RunningTaskInfo runningTaskInfo) {
        runningTaskInfo.setStopped(true);
        scheduledThreadPoolExecutor.remove(runningTaskInfo.getRunnable());
        if (runningTaskInfo.getFuture() != null) {
            runningTaskInfo.getFuture().cancel(true);
        }
    }

    /**
     * Set the future on the given task-info, watching for a possible race condition between setting the future and
     *  canceling the task.
     *
     * @param taskInfo
     * @param future
     */
    private void safeSetTaskInfoFuture(RunningTaskInfo taskInfo, Future<?> future) {
        taskInfo.setFuture(future);
        if (taskInfo.isStopped()) {
            future.cancel(true);
        }
    }

    // ========================================
    // Internal Classes
    // ----------------------------------------

    private class RecurringCronRunner implements Runnable {
        private final ExecutionTime executionTime;
        private final Runnable operation;
        private boolean shutdown = false;

        public RecurringCronRunner(ExecutionTime executionTime, Runnable operation) {
            this.executionTime = executionTime;
            this.operation = operation;
        }

        @Override
        public void run() {
            if (!shutdown) {
                try {
                    log.debug("Executing now");
                    operation.run();
                } catch (Exception exc) {
                    log.warn("task failure", exc);
                } finally {
                    if (!shutdown) {
                        log.debug("Scheduling next execution");
                        scheduleNextRun(executionTime, this);
                    } else {
                        log.debug("NOT scheduling next execution; was shutdown");
                    }
                }
            } else {
                log.debug("Skipping execution and NOT scheduling next one; was shutdown");
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    private class OneShotTaskWrapper implements Runnable {

        private final String taskId;
        private final Runnable nested;

        public OneShotTaskWrapper(String taskId, Runnable nested) {
            this.taskId = taskId;
            this.nested = nested;
        }

        @Override
        public void run() {
            try {
                nested.run();
            } finally {
                scheduledTasks.remove(taskId);
            }
        }
    }

    private static class RunningTaskInfo {
        @Getter
        private final Runnable runnable;

        @Getter
        @Setter
        private Future<?> future;

        @Getter
        @Setter
        private boolean stopped = false;

        public RunningTaskInfo(Runnable runnable) {
            this.runnable = runnable;
        }
    }
}
