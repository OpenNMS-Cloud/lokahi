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
package org.opennms.miniongateway.grpc.server;

import io.opentelemetry.api.OpenTelemetry;
import org.apache.ignite.Ignite;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TwinRpcHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcTwinPublisherConfig {

    @Value("${debug.span.full.message:false}")
    private boolean debugSpanFullMessage;

    @Value("${debug.span.content:false}")
    private boolean debugSpanContent;

    @Bean
    public ServerHandler serverHandler(
            GrpcTwinPublisher grpcTwinPublisher,
            TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
            LocationServerInterceptor locationServerInterceptor,
            OpenTelemetry openTelemetry) {
        return new TwinRpcHandler(
                grpcTwinPublisher,
                tenantIDGrpcServerInterceptor,
                locationServerInterceptor,
                openTelemetry.getTracer(getClass().getName()),
                debugSpanFullMessage,
                debugSpanContent);
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public GrpcTwinPublisher grpcTwinPublisher(Ignite ignite, OpenTelemetry openTelemetry) {
        return new GrpcTwinPublisher(
                ignite, openTelemetry.getTracer(getClass().getName()), debugSpanFullMessage, debugSpanContent);
    }
}
