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
            .usePlaintext().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public MinionCertificateManagerClient createMinionCertificateManagerClient(@Qualifier("minionCertificateManager") ManagedChannel channel) {
        return new MinionCertificateManagerClient(channel, deadline);
    }
}
