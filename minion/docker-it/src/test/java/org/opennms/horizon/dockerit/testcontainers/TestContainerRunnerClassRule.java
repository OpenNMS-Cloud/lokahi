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
package org.opennms.horizon.dockerit.testcontainers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import java.time.Duration;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestContainerRunnerClassRule extends ExternalResource {

    public static final String DEFAULT_APPLICATION_DOCKER_IMAGE_NAME = "opennms/lokahi-minion:latest";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestContainerRunnerClassRule.class);

    private Logger LOG = DEFAULT_LOGGER;

    private GenericContainer mockMinionGatewayContainer;
    private GenericContainer applicationContainer;
    private GenericContainer sslGatewayContainer;
    private static final int UDP_NETFLOW5_PORT = 8877;
    private static final int UDP_NETFLOW9_PORT = 4729;

    private Network network;

    public TestContainerRunnerClassRule() {
        String applicationDockerImageName = System.getProperty("application.docker.image");
        if (applicationDockerImageName == null) {
            applicationDockerImageName = DEFAULT_APPLICATION_DOCKER_IMAGE_NAME;
        }

        LOG.info("Using application docker image: name={}", applicationDockerImageName);

        mockMinionGatewayContainer = new GenericContainer(
                DockerImageName.parse("opennms/lokahi-mock-minion-gateway").withTag("latest"));
        applicationContainer = new GenericContainer(
                DockerImageName.parse(applicationDockerImageName).toString());
        sslGatewayContainer = new GenericContainer(
                DockerImageName.parse("nginx:1.21.6-alpine").toString());
    }

    @Override
    protected void before() {
        network = Network.newNetwork();

        LOG.info("USING TEST DOCKER NETWORK {}", network.getId());

        startMockMinionGatewayContainer();
        startSslGatewayContainer();
        startApplicationContainer();
    }

    @Override
    protected void after() {
        applicationContainer.stop();
        sslGatewayContainer.stop();
        mockMinionGatewayContainer.stop();
    }

    // ========================================
    // Container Startups
    // ----------------------------------------

    private void startMockMinionGatewayContainer() {

        mockMinionGatewayContainer
                .withNetwork(network)
                .withNetworkAliases("minion-gateway")
                .withExposedPorts(8080)
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("MOCK-MINION-GATEWAY"))
                .start();
    }

    private void startSslGatewayContainer() {
        sslGatewayContainer
                .withNetwork(network)
                .withNetworkAliases("opennms-minion-ssl-gateway")
                .withExposedPorts(443)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("ssl-gateway/nginx.conf"), "/etc/nginx/nginx.conf")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("ssl-gateway/CA.cert"), "/etc/ssl/cachain.pem")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("ssl-gateway/server.signed.cert"),
                        "/etc/nginx/minion-gateway.crt")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("ssl-gateway/server.key"), "/etc/nginx/minion-gateway.key")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("ssl-gateway/certificate-passwords"),
                        "/etc/nginx/cert-passwords")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("SSL-GATEWAY"))
                .start();
    }

    private void startApplicationContainer() {
        applicationContainer
                .withExposedPorts(8101, 8181, 5005)
                .withCreateContainerCmdModifier(cmd -> {
                    ((CreateContainerCmd) cmd).withHostName("test-minion-001");
                })
                .withNetwork(network)
                .withNetworkAliases("application", "application-host")
                .dependsOn(mockMinionGatewayContainer)
                .dependsOn(sslGatewayContainer)
                .withStartupTimeout(Duration.ofMinutes(5))
                .withEnv(
                        "JAVA_TOOL_OPTIONS",
                        "-Djava.security.egd=file:/dev/./urandom -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
                .withEnv("MINION_LOCATION", "Default")
                .withEnv("MINION_GATEWAY_HOST", "opennms-minion-ssl-gateway")
                .withEnv("GRPC_CLIENT_KEYSTORE_PASSWORD", "passw0rd")
                .withEnv("GRPC_CLIENT_TRUSTSTORE", "/opt/karaf/CA.cert")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("client/minion.p12"), "/opt/karaf/minion.p12")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("ssl-gateway/CA.cert"), "/opt/karaf/CA.cert")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"));

        // DEBUGGING: uncomment to force local port 5005 (NOTE: MAKE SURE IT IS COMMENTED-OUT AT CODE COMMIT-TIME - i.e.
        // on "git commit")
        // applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer
                .getPortBindings()
                .addAll(List.of(
                        ExposedPort.udp(UDP_NETFLOW5_PORT).toString(),
                        ExposedPort.udp(UDP_NETFLOW9_PORT).toString()));
        applicationContainer.start();

        var karafHttpPort = applicationContainer.getMappedPort(8181); // application-http-port
        var netflow5ListenerPort = getUdpPortBinding(UDP_NETFLOW5_PORT);
        var netflow9ListenerPort = getUdpPortBinding(UDP_NETFLOW9_PORT);
        var debuggerPort = applicationContainer.getMappedPort(5005);
        var mockMinionGatewayHttpPort = mockMinionGatewayContainer.getMappedPort(8080);

        LOG.info(
                "APPLICATION MAPPED PORTS: http={}; mock-minion-gateway-http-port={}; netflow 5 listener port={}; "
                        + "netflow 9 listener port={}, debugger={}",
                karafHttpPort,
                mockMinionGatewayHttpPort,
                netflow5ListenerPort,
                netflow9ListenerPort,
                debuggerPort);

        System.setProperty("application.base-url", "http://localhost:" + karafHttpPort);
        System.setProperty("application.host-name", "localhost");
        System.setProperty("netflow-5-listener-port", netflow5ListenerPort);
        System.setProperty("netflow-9-listener-port", netflow9ListenerPort);
        System.setProperty("mock-miniongateway.base-url", "http://localhost:" + mockMinionGatewayHttpPort);
    }

    private String getUdpPortBinding(int portNumber) {
        return applicationContainer
                .getContainerInfo()
                .getNetworkSettings()
                .getPorts()
                .getBindings()
                .get(new ExposedPort(portNumber, InternetProtocol.UDP))[1]
                .getHostPortSpec();
    }
}
