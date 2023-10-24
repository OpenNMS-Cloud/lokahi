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

package org.opennms.horizon.minioncertmanager.testcontainers;

import java.io.File;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.keycloak.common.util.Base64;
import org.opennms.horizon.minioncertmanager.certificate.CaCertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@SuppressWarnings("rawtypes")
public class TestContainerRunnerClassRule extends ExternalResource {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestContainerRunnerClassRule.class);

    private final String dockerImage = System.getProperty("application.docker.image");

    private Logger LOG = DEFAULT_LOGGER;

    private GenericContainer applicationContainer;

    private final GenericContainer<?> mockInventoryContainer;

    private Network network;

    private KeyPair jwtKeyPair;

    private TemporaryFolder tempDir = new TemporaryFolder();

    public TestContainerRunnerClassRule() {
        applicationContainer = new GenericContainer(DockerImageName.parse(dockerImage));
        mockInventoryContainer = new GenericContainer<>(DockerImageName.parse("tkpd/gripmock"));
    }

    @Override
    protected void before() throws Exception {
        network = Network.newNetwork();
        LOG.info("USING TEST DOCKER NETWORK {}", network.getId());

        try {
            jwtKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        var inventoryUrl = startInventoryContainer();
        LOG.info("Mock inventory server url: {}", inventoryUrl);

        startApplicationContainer(inventoryUrl);
    }

    @Override
    protected void after() {
        applicationContainer.stop();
        mockInventoryContainer.stop();
    }

//========================================
// Container Startups
//----------------------------------------

    private void startApplicationContainer(String inventoryUrl) throws Exception {
        // create temporary certificates
        tempDir.create();
        File temporarySecrets = tempDir.newFolder("mtls");
        CaCertificateGenerator.generate(temporarySecrets, "OU=openNMS Test,C=CA", 3600);
        LOG.info("Using temporary CA certificate located in {}", temporarySecrets);

        applicationContainer.withNetwork(network)
            .withNetworkAliases("application", "application-host")
            .withExposedPorts(8080, 8990, 5005)
            .withEnv("JAVA_TOOL_OPTIONS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            .withEnv("KEYCLOAK_PUBLIC_KEY", Base64.encodeBytes(jwtKeyPair.getPublic().getEncoded()))
            .withEnv("grpc.inventory.url", inventoryUrl)
            .withFileSystemBind(new File(temporarySecrets, "ca.crt").getAbsolutePath(), "/run/secrets/mtls/tls.crt")
            .withFileSystemBind(new File(temporarySecrets, "ca.key").getAbsolutePath(), "/run/secrets/mtls/tls.key")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"))
            .waitingFor(Wait.forLogMessage(".*Started MinionCertificateManagerApplication.*", 1)
                .withStartupTimeout(Duration.ofMinutes(1))
            );

        // DEBUGGING: uncomment to force local port 5005
        //applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer.start();

        var externalGrpcPort = applicationContainer.getMappedPort(8990); // application-external-grpc-port
        var externalHttpPort = applicationContainer.getMappedPort(8080);
        var debuggerPort = applicationContainer.getMappedPort(5005);

        LOG.info("APPLICATION MAPPED PORTS:  external-grpc={};  external-http={}; debugger={}", externalGrpcPort, externalHttpPort, debuggerPort);
        System.setProperty("application-external-grpc-port", String.valueOf(externalGrpcPort));
        System.setProperty("application-external-http-port", String.valueOf(externalHttpPort));
        System.setProperty("application-external-http-base-url", "http://localhost:" + externalHttpPort);
    }

    private String startInventoryContainer() {
        mockInventoryContainer
            .withNetwork(network)
            .withNetworkAliases("opennms-inventory")
            .withExposedPorts(4770)
            .withClasspathResourceMapping("proto", "/proto", BindMode.READ_ONLY)
            // refer from jar file
            .withClasspathResourceMapping("monitoringLocation.proto", "/proto/monitoringLocation.proto", BindMode.READ_ONLY)
            // make sure stub file don't have tail 0A return char
            .withCommand("--stub=/proto/stub", "/proto/monitoringLocation.proto")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("MOCK"))
            .waitingFor(Wait.forListeningPort()
                .withStartupTimeout(Duration.ofSeconds(10))
            );

        mockInventoryContainer.start();

        return "opennms-inventory:4770";
    }
}
