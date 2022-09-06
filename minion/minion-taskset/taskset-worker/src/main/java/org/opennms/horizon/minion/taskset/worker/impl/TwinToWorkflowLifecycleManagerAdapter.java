package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.taskset.worker.TaskLifecycleManager;
import org.opennms.horizon.minion.ipc.twin.api.TwinListener;
import org.opennms.taskset.contract.TaskSet;

public class TwinToWorkflowLifecycleManagerAdapter implements TwinListener<TaskSet> {

  private final TaskLifecycleManager workflowLifecycleManager;

  public TwinToWorkflowLifecycleManagerAdapter(TaskLifecycleManager workflowLifecycleManager) {
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
