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
package org.opennms.horizon.server.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.mapper.alert.AlertEventDefinitionMapper;
import org.opennms.horizon.server.mapper.alert.AlertsCountMapper;
import org.opennms.horizon.server.mapper.alert.MonitorPolicyMapper;
import org.opennms.horizon.server.service.flows.FlowClient;
import org.opennms.horizon.server.service.grpc.AlertsClient;
import org.opennms.horizon.server.service.grpc.EventsClient;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.service.grpc.NotificationClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GrpcConfig {
    @Value("${grpc.url.inventory}")
    private String inventoryGrpcAddress;

    @Value("${grpc.url.events}")
    private String eventsGrpcAddress;

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Value("${grpc.url.notification}")
    private String notificationGrpcAddress;

    @Value("${grpc.url.alerts}")
    private String alertsGrpcAddress;

    private final MonitorPolicyMapper policyMapper;

    private final AlertEventDefinitionMapper alertEventDefinitionMapper;

    private final AlertsCountMapper alertsCountMapper;

    @Value("${grpc.url.flows}")
    private String flowsQuerierGrpcAddress;

    @Value("${grpc.url.flows.tls.enabled:false}")
    private boolean flowsTlsEnabled;

    @Value("${grpc.url.minion-certificate-manager}")
    private String minionCertificateManagerGrpcAddress;

    @Bean
    public ServerHeaderUtil createHeaderUtil() {
        return new ServerHeaderUtil();
    }

    @Bean(name = "inventory")
    public ManagedChannel createInventoryChannel() {
        return ManagedChannelBuilder.forTarget(inventoryGrpcAddress)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(name = "events")
    public ManagedChannel createEventsChannel() {
        return ManagedChannelBuilder.forTarget(eventsGrpcAddress)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(name = "notification")
    public ManagedChannel createNotificationChannel() {
        return ManagedChannelBuilder.forTarget(notificationGrpcAddress)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(name = "alerts")
    public ManagedChannel createAlertsChannel() {
        return ManagedChannelBuilder.forTarget(alertsGrpcAddress)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(name = "flowQuerier")
    public ManagedChannel createFlowQuerierChannel() {
        var builder = ManagedChannelBuilder.forTarget(flowsQuerierGrpcAddress).keepAliveWithoutCalls(true);
        if (!flowsTlsEnabled) {
            builder.usePlaintext();
        }
        return builder.build();
    }

    @Bean(name = "minionCertificateManager")
    public ManagedChannel minionCertificateManagerChannel() {
        return ManagedChannelBuilder.forTarget(minionCertificateManagerGrpcAddress)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public InventoryClient createInventoryClient(@Qualifier("inventory") ManagedChannel channel) {
        return new InventoryClient(channel, deadline);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public EventsClient createEventsClient(@Qualifier("events") ManagedChannel channel) {
        return new EventsClient(channel, deadline);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public NotificationClient createNotificationClient(@Qualifier("notification") ManagedChannel channel) {
        return new NotificationClient(channel);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public AlertsClient createAlertsClient(@Qualifier("alerts") ManagedChannel channel) {
        return new AlertsClient(channel, deadline, policyMapper, alertEventDefinitionMapper, alertsCountMapper);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public FlowClient createFlowClient(
            @Qualifier("flowQuerier") ManagedChannel channel, InventoryClient inventoryClient) {
        return new FlowClient(inventoryClient, channel, deadline);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public MinionCertificateManagerClient createMinionCertificateManagerClient(
            @Qualifier("minionCertificateManager") ManagedChannel channel) {
        return new MinionCertificateManagerClient(channel, deadline);
    }
}
