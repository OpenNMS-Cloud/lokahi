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
package org.opennms.horizon.shared.grpc.common;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.opentelemetry.api.trace.Span;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.shared.constants.GrpcConstants;

// TODO: distinguish non-multi-tenant deployments of this code and skip?
@Slf4j
public class TenantIDGrpcServerInterceptor implements ServerInterceptor {
    private static final RateLimitedLog usingDefaultTenantIdLog = RateLimitedLog.withRateLimit(log)
            .maxRate(1)
            .every(Duration.ofMinutes(1))
            .build();

    /**
     * GRPC uses Context.Key objects to read the context (there are no direct methods on the context itself).  Define
     *  the Context.Key here for reuse.
     */
    @Getter
    private static final Context.Key<String> contextTenantId = GrpcConstants.TENANT_ID_CONTEXT_KEY;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata headers, ServerCallHandler<ReqT, RespT> callHandler) {
        // Read the tenant id out of the headers
        log.debug("Received metadata: {}", headers);
        String tenantId = commonReadContextTenantId(() -> headers.get(GrpcConstants.TENANT_ID_REQUEST_KEY));
        if (tenantId == null) {
            //
            // FAILED
            //
            log.error("Missing tenant id");

            serverCall.close(Status.UNAUTHENTICATED.withDescription("Missing tenant id"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        // Write the tenant ID to the current GRPC context
        Context context = Context.current().withValue(TenantIDGrpcServerInterceptor.contextTenantId, tenantId);
        return Contexts.interceptCall(context, serverCall, headers, callHandler);
    }

    public String readCurrentContextTenantId() {
        return commonReadContextTenantId(() -> contextTenantId.get());
    }

    public String readContextTenantId(Context context) {
        return commonReadContextTenantId(() -> contextTenantId.get(context));
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private String commonReadContextTenantId(Supplier<String> readTenantIdOp) {
        var tenantId = readTenantIdOp.get();
        var span = Span.current();
        if (span.isRecording()) {
            span.setAttribute("user", tenantId);
        }
        return tenantId;
    }
}
