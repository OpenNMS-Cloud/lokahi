/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.minioncertmanager.grpc;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
