package org.opennms.horizon.minion.taskset.worker;


import org.opennms.taskset.contract.TaskSet;

/**
 * Responsible for management of task lifecycle.
 * Ensure all elements from workflows/task set are processed.
 */
public interface TaskSetLifecycleManager {

  /**
   * Force deployment of a given task set.
   *
   * @param taskSet Task set.
   * @return Number of stopped tasks.
   */
  int deploy(TaskSet taskSet);

  /**
   * Return the last TaskSet that was successfully deployed
   *
   * @return active TaskSet
   */
  TaskSet getDeployedTaskSet();

}
