package org.opennms.horizon.minion.grpc;

import org.opennms.cloud.grpc.minion.CloudToMinionMessage;

public interface CloudMessageHandler {

    void handle(CloudToMinionMessage message);

}
