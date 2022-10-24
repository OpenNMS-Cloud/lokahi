package org.opennms.horizon.minion.taskset.worker;

public interface TaskExecutorLocalService {
    void start() throws Exception;
    void cancel();
}
