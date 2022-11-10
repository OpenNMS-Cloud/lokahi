package org.opennms.horizon.core.rpc.request.client;

import org.junit.Before;
import org.junit.Test;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("rawtypes")
public class RpcRequestGrpcClientFactoryTest {

    private RpcRequestGrpcClientFactory target;

    @Before
    public void setUp() {
        target = new RpcRequestGrpcClientFactory();
    }

    @Test
    public void testGetClientCustomDeserializer() {
        RpcClientFactory.Deserializer<RpcResponseProto> testDeserializer = msg -> msg;

        RpcRequestGrpcClient rpcRequestGrpcClient = commonTestGetClient(() -> target.getClient(testDeserializer));

        assertSame(testDeserializer, rpcRequestGrpcClient.getDeserializer());
    }

    @Test
    public void testGetClientDefaultSerializer() {
        commonTestGetClient(() -> target.getClient());
    }

//========================================
// Internals
//----------------------------------------

    private RpcRequestGrpcClient commonTestGetClient(Supplier<RpcClient> rpcClientGetter) {
        //
        // Execute
        //
        target.setHost("x-test-host-x");
        target.setPort(9999);
        target.setTlsEnabled(false);
        target.setMaxMessageSize(999888);
        RpcClient client = rpcClientGetter.get();

        //
        // Verify the Results
        //
        assertNotNull(client);
        assertTrue(client instanceof RpcRequestGrpcClient);
        RpcRequestGrpcClient rpcRequestGrpcClient = (RpcRequestGrpcClient) client;

        assertEquals("x-test-host-x", rpcRequestGrpcClient.getHost());
        assertEquals(9999, rpcRequestGrpcClient.getPort());
        assertFalse(rpcRequestGrpcClient.isTlsEnabled());
        assertEquals(999888, rpcRequestGrpcClient.getMaxMessageSize());

        return rpcRequestGrpcClient;
    }

}
