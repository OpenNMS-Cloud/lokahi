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
package org.opennms.horizon.alertservice.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.TokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.TokenUtil;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertServerInterceptor implements ServerInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(AlertServerInterceptor.class);
    private static final String TOKEN_PREFIX = "Bearer";
    private final KeycloakDeployment keycloak;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata headers, ServerCallHandler<ReqT, RespT> callHandler) {
        String authHeader = headers.get(GrpcConstants.AUTHORIZATION_METADATA_KEY);
        try {
            Optional<String> tenantId = verifyAccessToken(authHeader);
            Context context = tenantId.map(
                            tnId -> Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tnId))
                    .orElseThrow();
            return Contexts.interceptCall(context, serverCall, headers, callHandler);
        } catch (VerificationException e) {
            LOG.error("Failed to verify access token", e);
            serverCall.close(Status.UNAUTHENTICATED.withDescription("Invalid access token"), new Metadata());
            return new ServerCall.Listener<>() {};
        } catch (NoSuchElementException e) {
            serverCall.close(Status.UNAUTHENTICATED.withDescription("Missing tenant id"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
    }

    protected Optional<String> verifyAccessToken(String authHeader) throws VerificationException {
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            throw new VerificationException();
        }
        String token = authHeader.substring(TOKEN_PREFIX.length() + 1);
        TokenVerifier<AccessToken> verifier =
                AdapterTokenVerifier.createVerifier(token, keycloak, false, AccessToken.class);
        verifier.withChecks(
                TokenVerifier.SUBJECT_EXISTS_CHECK,
                new TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_BEARER),
                TokenVerifier.IS_ACTIVE);
        verifier.verify();
        AccessToken accessToken = verifier.getToken();
        return Optional.ofNullable((String) accessToken.getOtherClaims().get(GrpcConstants.TENANT_ID_KEY));
    }
}
