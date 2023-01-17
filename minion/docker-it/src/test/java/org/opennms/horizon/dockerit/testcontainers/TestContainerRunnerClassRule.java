/*
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
 */

package org.opennms.horizon.dockerit.testcontainers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@SuppressWarnings("rawtypes")
public class TestContainerRunnerClassRule extends ExternalResource {

    public static final String DEFAULT_APPLICATION_DOCKER_IMAGE_NAME = "opennms/horizon-stream-minion:latest";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestContainerRunnerClassRule.class);

    private Logger LOG = DEFAULT_LOGGER;

    private GenericContainer mockMinionGatewayContainer;
    private GenericContainer applicationContainer;

    private Network network;

    public TestContainerRunnerClassRule() {
        String applicationDockerImageName = System.getProperty("application.docker.image");
        if (applicationDockerImageName == null) {
            applicationDockerImageName = DEFAULT_APPLICATION_DOCKER_IMAGE_NAME;
        }

        LOG.info("Using application docker image: name={}", applicationDockerImageName);

        mockMinionGatewayContainer = new GenericContainer(DockerImageName.parse("opennms/horizon-stream-mock-minion-gateway").withTag("latest"));
        applicationContainer = new GenericContainer(DockerImageName.parse(applicationDockerImageName).toString());
    }

    @Override
    protected void before() throws Throwable {
        network =
            Network.newNetwork()
            ;

        LOG.info("USING TEST DOCKER NETWORK {}", network.getId());

        startMockMinionGatewayContainer();
        startApplicationContainer();
    }

    @Override
    protected void after() {
        applicationContainer.stop();
        mockMinionGatewayContainer.stop();
    }

//========================================
// Container Startups
//----------------------------------------

    private void startMockMinionGatewayContainer() {

        mockMinionGatewayContainer
            .withNetwork(network)
            .withNetworkAliases("minion-gateway")
            .withExposedPorts(8080)
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("MOCK-MINION-GATEWAY"))
        ;

    }

    private void startApplicationContainer() {
        applicationContainer
            .withCreateContainerCmdModifier(cmd -> ((CreateContainerCmd) cmd).withHostName("test-minion-001")) // Why is the cast necessary?
            .withNetwork(network)
            .withNetworkAliases("application", "application-host")
            .dependsOn(mockMinionGatewayContainer)
            .withExposedPorts(8101, 8181, 5005)
            .withStartupTimeout(Duration.ofMinutes(5))
            .withEnv("JAVA_TOOL_OPTIONS", "-Djava.security.egd=file:/dev/./urandom -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            .withEnv("MINION_LOCATION", "Default")
            .withEnv("USE_KUBERNETES", "false")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"))
            ;

        // DEBUGGING: uncomment to force local port 5005
        // applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer.start();

        var karafHttpPort = applicationContainer.getMappedPort(8181); // application-http-port
        var debuggerPort = applicationContainer.getMappedPort(5005);
        var mockMinionGatewayHttpPort = mockMinionGatewayContainer.getMappedPort(8080);

        LOG.info("APPLICATION MAPPED PORTS: http={}; mock-minion-gateway-http-port={}; debugger={}",
            karafHttpPort,
            mockMinionGatewayHttpPort,
            debuggerPort
            );

        System.setProperty("application.base-url", "http://localhost:" + karafHttpPort);
        System.setProperty("mock-miniongateway.base-url", "http://localhost:" + mockMinionGatewayHttpPort);
    }
}
