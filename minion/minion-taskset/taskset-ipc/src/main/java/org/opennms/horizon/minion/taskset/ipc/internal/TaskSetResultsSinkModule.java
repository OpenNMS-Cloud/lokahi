package org.opennms.horizon.minion.taskset.ipc.internal;

import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.taskset.contract.TaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink Module for processing TaskSetResults.  Note this is used in the communication between the Minion and
 * Minion Gateway, so Tenant IDs are not explicitly handled here.
 */
public class TaskSetResultsSinkModule implements SinkModule<TaskSetResults, TaskSetResults> {

  public static final String MODULE_ID = "task-set-result";
  private final Logger logger = LoggerFactory.getLogger(TaskSetResultsSinkModule.class);

  @Override
  public String getId() {
    return MODULE_ID;
  }

  @Override
  public int getNumConsumerThreads() {
    return 0;
  }

  @Override
  public byte[] marshal(TaskSetResults resultsMessage) {
    try {
      return resultsMessage.toByteArray();
    } catch (Exception e) {
      logger.warn("Error while marshalling message {}.", resultsMessage, e);
      return new byte[0];
    }
  }

  @Override
  public TaskSetResults unmarshal(byte[] bytes) {
    try {
      return TaskSetResults.parseFrom(bytes);
    } catch (Exception e) {
      logger.warn("Error while unmarshalling message.", e);
      return null;
    }
  }

  @Override
  public byte[] marshalSingleMessage(TaskSetResults resultsMessage) {
    return marshal(resultsMessage);
  }

  @Override
  public TaskSetResults unmarshalSingleMessage(byte[] bytes) {
    return unmarshal(bytes);
  }

  @Override
  public AggregationPolicy<TaskSetResults, TaskSetResults, ?> getAggregationPolicy() {
    return new IdentityAggregationPolicy<>();
  }

  @Override
  public AsyncPolicy getAsyncPolicy() {
    return new AsyncPolicy() {
      public int getQueueSize() {
        return 10;
      }

      public int getNumThreads() {
        return 10;
      }

    };
  }

}
