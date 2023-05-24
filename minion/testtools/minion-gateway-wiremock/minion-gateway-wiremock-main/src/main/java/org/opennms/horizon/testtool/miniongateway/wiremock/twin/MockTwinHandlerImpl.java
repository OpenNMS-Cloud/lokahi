package org.opennms.horizon.testtool.miniongateway.wiremock.twin;

import com.google.protobuf.ByteString;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockGrpcServiceApi;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockTwinHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class MockTwinHandlerImpl implements MockTwinHandler {

    @Autowired
    private MockGrpcServiceApi grpcOperations;

    private AtomicLong sessionSeq = new AtomicLong(0);

    @Override
    public void publish(String topic, byte[] content) {
        TwinResponseProto twinResponseProto =
            TwinResponseProto.newBuilder()
                .setConsumerKey(topic)
                .setTwinObject(ByteString.copyFrom(content))
                .setSessionId("mock-twin-handler-session-" + sessionSeq.getAndIncrement())
                .build()
                ;

        CloudToMinionMessage cloudToMinionMessage =
            CloudToMinionMessage.newBuilder()
                .setTwinResponse(twinResponseProto)
                .build()
            ;

        grpcOperations.sendMessageToLocation(cloudToMinionMessage);
    }
}
