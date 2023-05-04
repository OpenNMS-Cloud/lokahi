package org.opennms.horizon.minion.plugin.registration;

import org.opennms.horizon.minion.registration.proto.PluginConfigMessage;
import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginConfigSinkModule implements SinkModule<PluginConfigMessage, PluginConfigMessage> {

  public static final String MODULE_ID = "workflows";
  private final Logger logger = LoggerFactory.getLogger(PluginConfigSinkModule.class);

  @Override
  public String getId() {
    return MODULE_ID;
  }

  @Override
  public int getNumConsumerThreads() {
    return 0;
  }

  @Override
  public byte[] marshal(PluginConfigMessage resultsMessage) {
    try {
      return resultsMessage.toByteArray();
    } catch (Exception e) {
      logger.warn("Error while marshalling message {}.", resultsMessage, e);
      return new byte[0];
    }
  }

  @Override
  public PluginConfigMessage unmarshal(byte[] bytes) {
    try {
      return PluginConfigMessage.parseFrom(bytes);
    } catch (Exception e) {
      logger.warn("Error while unmarshalling message.", e);
      return null;
    }
  }

  @Override
  public byte[] marshalSingleMessage(PluginConfigMessage resultsMessage) {
    return marshal(resultsMessage);
  }

  @Override
  public PluginConfigMessage unmarshalSingleMessage(byte[] bytes) {
    return unmarshal(bytes);
  }

  @Override
  public AggregationPolicy<PluginConfigMessage, PluginConfigMessage, ?> getAggregationPolicy() {
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
