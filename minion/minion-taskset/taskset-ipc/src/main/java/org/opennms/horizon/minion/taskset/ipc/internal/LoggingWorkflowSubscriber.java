package org.opennms.horizon.minion.taskset.ipc.internal;

import org.opennms.horizon.minion.ipc.twin.api.TwinListener;
import org.opennms.taskset.contract.TaskSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingWorkflowSubscriber implements TwinListener<TaskSet> {

  private final Logger logger = LoggerFactory.getLogger(LoggingWorkflowSubscriber.class);

  @Override
  public void accept(TaskSet taskSet) {
    logger.info(">> Received task set twin {}", taskSet);
  }

  @Override
  public Class<TaskSet> getType() {
    return TaskSet.class;
  }
}
