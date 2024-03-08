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
package org.opennms.horizon.flows.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

@TestConfiguration
@ImportAutoConfiguration
public class IngestorApplicationConfig {

    public static final String SERVER_NAME = InProcessServerBuilder.generateName();

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Bean
    public GrpcIngesterMockServer grpcIngesterMockServer() {
        return new GrpcIngesterMockServer();
    }

    @Bean(name = "ingestorChannel")
    public ManagedChannel createIngestorChannel() {
        return InProcessChannelBuilder.forName(SERVER_NAME).directExecutor().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initStubs")
    public IngestorClient createIngestorClient(
            @Qualifier("ingestorChannel") ManagedChannel channel, RetryTemplate retryTemplate) {
        return new IngestorClient(channel, deadline, retryTemplate);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate();
    }
}
