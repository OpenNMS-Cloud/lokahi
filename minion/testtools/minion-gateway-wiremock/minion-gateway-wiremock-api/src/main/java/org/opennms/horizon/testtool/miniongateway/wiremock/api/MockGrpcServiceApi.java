package org.opennms.horizon.testtool.miniongateway.wiremock.api;

import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.SinkMessage;

import java.util.List;

public interface MockGrpcServiceApi {
    void sendMessageToLocation(CloudToMinionMessage message);
    void sendMessageToMinion(String minionId, CloudToMinionMessage message);
    List<Identity> getConnectedMinions();
    List<SinkMessage> getReceivedSinkMessages();
}
