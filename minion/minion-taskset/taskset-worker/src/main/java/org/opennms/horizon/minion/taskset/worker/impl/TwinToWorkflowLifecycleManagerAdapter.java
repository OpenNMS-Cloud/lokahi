package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.taskset.worker.TaskSetLifecycleManager;
import org.opennms.horizon.minion.ipc.twin.api.TwinListener;
import org.opennms.taskset.contract.TaskSet;

public class TwinToWorkflowLifecycleManagerAdapter implements TwinListener<TaskSet> {

  private final TaskSetLifecycleManager workflowLifecycleManager;

  public TwinToWorkflowLifecycleManagerAdapter(TaskSetLifecycleManager workflowLifecycleManager) {
    this.workflowLifecycleManager = workflowLifecycleManager;
  }

  @Override
  public Class<TaskSet> getType() {
    return TaskSet.class;
  }

  @Override
  public void accept(TaskSet taskSet) {
    workflowLifecycleManager.deploy(taskSet);
  }
}
