package org.opennms.horizon.minion.echo.ipc.client;

import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;

public interface LocationAwareEchoClient {

    CompletableFuture<EchoResponse> execute(String systemId, String location, Long timeToLive, EchoRequest payload);
}
