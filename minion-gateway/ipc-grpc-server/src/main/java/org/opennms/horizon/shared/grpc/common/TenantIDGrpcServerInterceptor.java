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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.Supplier;

// TODO: distinguish non-multi-tenant deployments of this code and skip?
@Slf4j
public class TenantIDGrpcServerInterceptor implements ServerInterceptor {

    // TODO / TBD888 : REMOVE default tenant id code
    public static final String DEFAULT_TENANT_ID = "opennms-prime";

    private static final RateLimitedLog usingDefaultTenantIdLog =
        RateLimitedLog
            .withRateLimit(log)
            .maxRate(1)
            .every(Duration.ofMinutes(1))
            .build();

    //TODO will change to JWT token
    private static final Metadata.Key HEADER_KE = Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * GRPC uses Context.Key objects to read the context (there are no direct methods on the context itself).  Define
     *  the Context.Key here for reuse.
     */
    @Getter
    private static final Context.Key<String> contextTenantId = Context.key("tenant-id");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata headers, ServerCallHandler<ReqT, RespT> callHandler) {
        // Read the tenant id out of the headers
        log.debug("Received metadata: {}", headers);
        String tenantId = commonReadContextTenantId(() -> headers.get(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER)));
        // TBD888: restore this logic when tenant ID is reliably received from the Minion upstream flow
        // if (tenantId == null) {
        //     //
        //     // FAILED
        //     //
        //     log.error("Missing tenant id");
        //
        //     serverCall.close(Status.UNAUTHENTICATED.withDescription("Missing tenant id"), new Metadata());
        //     return new ServerCall.Listener<>() {};
        // }

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
        String result = readTenantIdOp.get();

        // TODO / TBD888: REMOVE THIS ONCE RECEIVED PROPERLY FROM THE MINION
        if (result == null) {
            usingDefaultTenantIdLog.warn("!!! USING DEFAULT TENANT ID !!!");
            result = DEFAULT_TENANT_ID;
        }

        return result;
    }
}
