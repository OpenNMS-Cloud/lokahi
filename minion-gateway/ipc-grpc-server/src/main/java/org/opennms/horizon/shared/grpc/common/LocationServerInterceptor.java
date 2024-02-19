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

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.shared.constants.GrpcConstants;

/**
 * Location resolver which rely on grpc header.
 */
@Slf4j
public class LocationServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata headers, ServerCallHandler<ReqT, RespT> callHandler) {
        log.debug("Received metadata: {}", headers);
        String locationId = headers.get(GrpcConstants.LOCATION_ID_REQUEST_KEY);
        if (locationId == null) {
            //
            // FAILED
            //
            log.error("Missing location");

            serverCall.close(Status.UNAUTHENTICATED.withDescription("Missing location"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        // Write the tenant ID to the current GRPC context
        Context context = Context.current().withValue(GrpcConstants.LOCATION_ID_CONTEXT_KEY, locationId);
        return Contexts.interceptCall(context, serverCall, headers, callHandler);
    }

    public String readCurrentContextLocationId() {
        return GrpcConstants.LOCATION_ID_CONTEXT_KEY.get();
    }

    public String readContextLocation(Context context) {
        var locationId = GrpcConstants.LOCATION_ID_CONTEXT_KEY.get(context);
        var span = Span.current();
        if (span.isRecording()) {
            span.setAttribute("location-id", locationId);
        }
        return locationId;
    }
}
