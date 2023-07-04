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

package org.opennms.horizon.testcontainers;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@SuppressWarnings("rawtypes")
public class TestContainerRunnerClassRule extends ExternalResource {

    public static final String KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME = "kafka.bootstrap-servers";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestContainerRunnerClassRule.class);
    private final PostgreSQLContainer postgreSQLContainer;

    private Logger LOG = DEFAULT_LOGGER;

    private final String dockerImage = System.getProperty("application.docker.image");

    private String confluentPlatformVersion = "7.3.0";

    private KafkaContainer kafkaContainer;
    private GenericContainer applicationContainer;

    private Network network;

    public TestContainerRunnerClassRule() {
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion));
        postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.5-alpine");
        applicationContainer = new GenericContainer(DockerImageName.parse(dockerImage).toString());
    }

    @Override
    protected void before() throws Throwable {
        network =
            Network.newNetwork()
            ;

        LOG.info("USING TEST DOCKER NETWORK {}", network.getId());

        startKafkaContainer();
        startPostgresqlContainer();
        startApplicationContainer();
    }

    @Override
    protected void after() {
        applicationContainer.stop();
        postgreSQLContainer.stop();
        kafkaContainer.stop();
    }

//========================================
// Container Startups
//----------------------------------------

    private void startKafkaContainer() {
        kafkaContainer
            .withEmbeddedZookeeper()
            .withNetwork(network)
            .withNetworkAliases("kafka", "kafka-host")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("KAFKA"))
            .start();
        ;

        String bootstrapServers = kafkaContainer.getBootstrapServers();
        System.setProperty(KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME, bootstrapServers);
        LOG.info("KAFKA LOCALHOST BOOTSTRAP SERVERS {}", bootstrapServers);
    }

    private void startPostgresqlContainer() {
        postgreSQLContainer
            .withDatabaseName("minion_gateway")
            .withUsername("ignite")
            .withPassword("ignite")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("POSTGRES"))
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .start();
        LOG.info("PostgresSQL container started and available at {}", postgreSQLContainer.getJdbcUrl());
    }

    private void startApplicationContainer() {
        applicationContainer
            .withNetwork(network)
            .withNetworkAliases("application", "application-host")
            .withExposedPorts(8080, 8990, 8991)
            // .withExposedPorts(8080, 8990, 8991, 5005)
            .withStartupTimeout(Duration.ofMinutes(5))
            .withEnv("JAVA_TOOL_OPTIONS", "-Djava.security.egd=file:/dev/./urandom -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka-host:9092")
            .withEnv("IGNITE_USE_KUBERNETES", "false")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"))
            ;

        // DEBUGGING: uncomment to force local port 5005, and remove 5005 from .withExposedPorts() above
        // applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer.start();

        var httpPort = applicationContainer.getMappedPort(8080); // application-http-port
        var externalGrpcPort = applicationContainer.getMappedPort(8990); // application-external-grpc-port
        var internalGrpcPort = applicationContainer.getMappedPort(8991); // application-internal-grpc-port

        LOG.info("APPLICATION MAPPED PORTS: http={}; external-grpc={}; internal-grpc={};",
            httpPort,
            externalGrpcPort,
            internalGrpcPort
            );

        System.setProperty("application.base-url", "http://localhost:" + httpPort);
        System.setProperty("application-external-grpc-port", String.valueOf(externalGrpcPort));
        System.setProperty("application-internal-grpc-port", String.valueOf(internalGrpcPort));
    }
}
