/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.shared.grpc.common;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.opentelemetry.api.trace.Span;

import java.time.Duration;
import java.util.function.Supplier;

import org.opennms.horizon.shared.constants.GrpcConstants;

// TODO: distinguish non-multi-tenant deployments of this code and skip?
@Slf4j
public class TenantIDGrpcServerInterceptor implements ServerInterceptor {
    private static final RateLimitedLog usingDefaultTenantIdLog =
        RateLimitedLog
            .withRateLimit(log)
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
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata headers, ServerCallHandler<ReqT, RespT> callHandler) {
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

//========================================
// Internals
//----------------------------------------

    private String commonReadContextTenantId(Supplier<String> readTenantIdOp) {
        var tenantId = readTenantIdOp.get();
        var span = Span.current();
        if (span.isRecording()) {
            span.setAttribute("user", tenantId);
        }
        return tenantId;
    }
}
