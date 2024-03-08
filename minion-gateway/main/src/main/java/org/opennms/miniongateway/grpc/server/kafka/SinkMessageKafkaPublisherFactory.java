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
package org.opennms.miniongateway.grpc.server.kafka;

import com.google.protobuf.Message;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SinkMessageKafkaPublisherFactory {

    private final TenantIDGrpcServerInterceptor tenantInterceptor;
    private final LocationServerInterceptor locationInterceptor;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public <I extends Message, O extends Message> SinkMessageKafkaPublisher<I, O> create(
            SinkMessageMapper<I, O> mapper, String topic) {
        return new SinkMessageKafkaPublisher<>(
                kafkaTemplate, tenantInterceptor, locationInterceptor, mapper, topic, meterRegistry);
    }
}
