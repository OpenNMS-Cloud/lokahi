/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.miniongateway.ignite;

import static org.junit.Assert.*;

import com.google.protobuf.Any;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestDispatcher;

public class LocalIgniteRpcRequestDispatcherTest {

    private LocalIgniteRpcRequestDispatcher target;

    private RpcRequestDispatcher mockRpcRequestDispatcher;

    private CompletableFuture<GatewayRpcResponseProto> testCompletableFuture;
    private Any testAny;

    @Before
    public void setUp() throws Exception {
        mockRpcRequestDispatcher = Mockito.mock(RpcRequestDispatcher.class);

        testCompletableFuture = new CompletableFuture<>();
        testAny = Any.newBuilder().build();

        target = new LocalIgniteRpcRequestDispatcher(mockRpcRequestDispatcher);
    }

    @Test
    public void testDispatchBlankSystemId() {
        //
        // Setup Test Data and Interactions
        //
        var testRequest = GatewayRpcRequestProto.newBuilder()
                .setRpcId("x-rpc-id-x")
                .setModuleId("x-module-id-x")
                .setIdentity(MinionIdentity.newBuilder()
                        .setTenantId("x-tenant-id-x")
                        .setLocationId("x-location-x")
                        .setSystemId("")
                        .build())
                .setPayload(testAny)
                .build();

        var requestProtoMatcher = prepareRequestProtoMatcher("x-rpc-id-x", "x-module-id-x", testAny);
        Mockito.when(mockRpcRequestDispatcher.dispatch(
                        Mockito.eq("x-tenant-id-x"), Mockito.eq("x-location-x"), Mockito.argThat(requestProtoMatcher)))
                .thenReturn(testCompletableFuture);

        //
        // Execute
        //
        CompletableFuture<GatewayRpcResponseProto> result = target.execute(testRequest);

        //
        // Verify the Results
        //
        assertSame(testCompletableFuture, result);
    }

    @Test
    public void testDispatchNonBlankSystemId() {
        //
        // Setup Test Data and Interactions
        //
        var testRequest = GatewayRpcRequestProto.newBuilder()
                .setRpcId("x-rpc-id-x")
                .setModuleId("x-module-id-x")
                .setIdentity(MinionIdentity.newBuilder()
                        .setTenantId("x-tenant-id-x")
                        .setLocationId("x-location-x")
                        .setSystemId("x-system-id-x")
                        .build())
                .setPayload(testAny)
                .build();

        var requestProtoMatcher = prepareRequestProtoMatcher("x-rpc-id-x", "x-module-id-x", testAny);
        Mockito.when(mockRpcRequestDispatcher.dispatch(
                        Mockito.eq("x-tenant-id-x"),
                        Mockito.eq("x-location-x"),
                        Mockito.eq("x-system-id-x"),
                        Mockito.argThat(requestProtoMatcher)))
                .thenReturn(testCompletableFuture);

        //
        // Execute
        //
        CompletableFuture<GatewayRpcResponseProto> result = target.execute(testRequest);

        //
        // Verify the Results
        //
        assertSame(testCompletableFuture, result);
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private ArgumentMatcher<RpcRequestProto> prepareRequestProtoMatcher(String rpcId, String moduleId, Any payload) {
        return (argument) -> ((Objects.equals(rpcId, argument.getRpcId()))
                && (Objects.equals(moduleId, argument.getModuleId()))
                && (payload == argument.getPayload()));
    }
}
