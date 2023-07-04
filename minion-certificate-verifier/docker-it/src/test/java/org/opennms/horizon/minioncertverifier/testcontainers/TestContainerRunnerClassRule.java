/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
 */

package org.opennms.horizon.minioncertverifier.testcontainers;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class TestContainerRunnerClassRule extends ExternalResource {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestContainerRunnerClassRule.class);

    private final String dockerImage = System.getProperty("application.docker.image");

    private Logger LOG = DEFAULT_LOGGER;

    private final GenericContainer<?> applicationContainer;

    private final GenericContainer<?> mockCertificateManagerContainer;

    private Network network;

    public TestContainerRunnerClassRule() {
        applicationContainer = new GenericContainer<>(DockerImageName.parse(dockerImage));
        mockCertificateManagerContainer = new GenericContainer<>(DockerImageName.parse("tkpd/gripmock"));
    }

    @Override
    protected void before() throws Exception {
        network = Network.newNetwork();
        LOG.info("USING TEST DOCKER NETWORK {}", network.getId());

        var managerUrl = startCertificateManagerContainer();
        LOG.info("Mock server url: {}", managerUrl);
        startApplicationContainer(managerUrl);
    }

    @Override
    protected void after() {
        applicationContainer.stop();
        mockCertificateManagerContainer.stop();
    }

//========================================
// Container Startups
//----------------------------------------

    private void startApplicationContainer(String managerUrl) {
        applicationContainer.withNetwork(network)
            .withNetworkAliases("application", "application-host")
            .withExposedPorts(8080, 5005)
            .withEnv("JAVA_TOOL_OPTIONS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            .withEnv("grpc.url.minion-certificate-manager", managerUrl)
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"))
            .waitingFor(Wait.forLogMessage(".*Started MinionCertificateVerifier.*", 1)
                .withStartupTimeout(Duration.ofMinutes(1))
            );

        // DEBUGGING: uncomment to force local port 5005
        // applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer.start();

        var externalHttpPort = applicationContainer.getMappedPort(8080);
        var debuggerPort = applicationContainer.getMappedPort(5005);

        LOG.info("APPLICATION MAPPED PORTS: external-http={}; debugger={}", externalHttpPort, debuggerPort);
        System.setProperty("application-external-http-port", String.valueOf(externalHttpPort));
        System.setProperty("application-external-http-base-url", "http://localhost:" + externalHttpPort);
    }

    private String startCertificateManagerContainer() {
        mockCertificateManagerContainer
            .withNetwork(network)
            .withNetworkAliases("opennms-minion-certificate-manager")
            .withExposedPorts(4770)
            .withClasspathResourceMapping("proto", "/proto", BindMode.READ_ONLY)
            // refer from jar file
            .withClasspathResourceMapping("minion-certificate-manager.proto", "/proto/minion-certificate-manager.proto", BindMode.READ_ONLY)
            // make sure stub file don't have tail 0A return char
            .withCommand("--stub=/proto/stub", "/proto/minion-certificate-manager.proto")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("MOCK"))
            .waitingFor(Wait.forListeningPort()
                .withStartupTimeout(Duration.ofSeconds(10))
            );

        mockCertificateManagerContainer.start();

        return "opennms-minion-certificate-manager:4770";
    }
}
