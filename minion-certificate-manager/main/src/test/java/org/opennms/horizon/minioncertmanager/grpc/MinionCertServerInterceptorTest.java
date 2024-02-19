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
package org.opennms.horizon.minioncertmanager.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.shared.constants.GrpcConstants;

class MinionCertServerInterceptorTest {

    private final KeycloakDeployment keycloakDeployment = mock(KeycloakDeployment.class);
    private MinionCertServerInterceptor interceptor;
    private final ServerCall serverCall = mock(ServerCall.class);
    private final MethodDescriptor methodDescriptor = mock(MethodDescriptor.class);

    @BeforeEach
    void setup() {
        Set<String> bypassTokenMethods = new HashSet<>();
        bypassTokenMethods.add("bypassMethod");
        interceptor = new MinionCertServerInterceptor(keycloakDeployment, bypassTokenMethods);

        when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);
    }

    @Test
    void testInterceptor() throws VerificationException {
        when(methodDescriptor.getBareMethodName()).thenReturn("nonBypassMethod");
        String accessToken = "fake access token";

        Metadata headers = new Metadata();
        headers.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ServerCallHandler callHandler = mock(ServerCallHandler.class);

        var spyInterceptor = spy(interceptor);
        spyInterceptor.interceptCall(serverCall, headers, callHandler);
        verify(spyInterceptor, times(1)).verifyAccessToken(accessToken);
    }

    @Test
    void testBypassInterceptor() throws VerificationException {
        when(methodDescriptor.getBareMethodName()).thenReturn("bypassMethod");

        Metadata headers = new Metadata();
        ServerCallHandler callHandler = mock(ServerCallHandler.class);

        var spyInterceptor = spy(interceptor);
        spyInterceptor.interceptCall(serverCall, headers, callHandler);
        verify(spyInterceptor, times(0)).verifyAccessToken(any(String.class));
    }
}
