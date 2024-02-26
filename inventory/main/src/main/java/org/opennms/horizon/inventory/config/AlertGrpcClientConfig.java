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
package org.opennms.horizon.inventory.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.opennms.horizon.inventory.component.AlertClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertGrpcClientConfig {

    public static final String ALERT_GRPC_CHANNEL = "alert-channel";

    @Value("${grpc.client.alert.deadline:60000}")
    private long deadline;

    @Value("${grpc.client.alert.url}")
    private String alertUrl;

    @Bean(name = ALERT_GRPC_CHANNEL)
    public ManagedChannel createAlertChannel() {
        return ManagedChannelBuilder.forTarget(alertUrl)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public AlertClient createAlertClient(@Qualifier(ALERT_GRPC_CHANNEL) ManagedChannel channel) {
        return new AlertClient(channel, deadline);
    }
}
