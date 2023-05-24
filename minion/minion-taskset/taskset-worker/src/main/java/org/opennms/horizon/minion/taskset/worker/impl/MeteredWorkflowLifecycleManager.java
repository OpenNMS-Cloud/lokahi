package org.opennms.horizon.minion.taskset.worker.impl;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.Collections;
import java.util.Map;

import org.opennms.horizon.minion.taskset.worker.TaskSetLifecycleManager;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.taskset.contract.TaskSet;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

public class MeteredWorkflowLifecycleManager implements TaskSetLifecycleManager, MetricSet {

  private final IpcIdentity identity;
  private final TaskSetLifecycleManager delegate;
  private final Counter counter = new Counter();

  public MeteredWorkflowLifecycleManager(IpcIdentity identity, TaskSetLifecycleManager delegate) {
    this.identity = identity;
    this.delegate = delegate;
  }

  @Override
  public int deploy(TaskSet taskSet) {
    int size = taskSet.getTaskDefinitionList().size();
    int deployed = delegate.deploy(taskSet);
    counter.inc(size - deployed);
    return deployed;
  }

  @Override
  public TaskSet getDeployedTaskSet() {
    return delegate.getDeployedTaskSet();
  }

    @Override
  public Map<String, Metric> getMetrics() {
    return Collections.singletonMap(name("minion", identity.getId(), "taskset"), counter);
  }

}
