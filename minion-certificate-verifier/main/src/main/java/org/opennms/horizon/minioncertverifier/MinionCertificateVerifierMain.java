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
package org.opennms.horizon.minioncertverifier;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.opennms.horizon.minioncertverifier.controller.MinionCertificateManagerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Certificate Verifier entry point.
 */
@SpringBootConfiguration
@SpringBootApplication
public class MinionCertificateVerifierMain {

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Value("${grpc.url.minion-certificate-manager}")
    private String minionCertificateManagerUrl;

    public static void main(String[] args) {
        SpringApplication.run(MinionCertificateVerifierMain.class, args);
    }

    @Bean(name = "minionCertificateManager")
    public ManagedChannel minionCertificateManagerChannel() {
        return ManagedChannelBuilder.forTarget(minionCertificateManagerUrl)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public MinionCertificateManagerClient createMinionCertificateManagerClient(
            @Qualifier("minionCertificateManager") ManagedChannel channel) {
        return new MinionCertificateManagerClient(channel, deadline);
    }
}
