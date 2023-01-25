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

package org.opennms.horizon.alarmservice.testcontainers;

import java.time.Duration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("rawtypes")
public class TestContainerRunnerClassRule extends ExternalResource {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestContainerRunnerClassRule.class);

    private final String KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME = "kafka.bootstrap-servers";

    private final String dockerImage = System.getProperty("application.docker.image");

    private Logger LOG = DEFAULT_LOGGER;

    private String confluentPlatformVersion = "7.3.0";

    private KafkaContainer kafkaContainer;
    private GenericContainer applicationContainer;
    private PostgreSQLContainer postgreSQLContainer;

    private Network network;

    public TestContainerRunnerClassRule() {
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion));
        applicationContainer = new GenericContainer(DockerImageName.parse(dockerImage).toString());
        postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse("postgres").withTag("14.5-alpine").toString());
    }

    @Override
    protected void before() throws Throwable {
        network =
            Network.newNetwork()
            ;

        LOG.info("USING TEST DOCKER NETWORK {}", network.getId());

        startKafkaContainer();
        startPostgresContainer();
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

    private void startPostgresContainer() {

        postgreSQLContainer
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withEnv("POSTGRESS_CLIENT_PORT", String.valueOf(PostgreSQLContainer.POSTGRESQL_PORT))
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("POSTGRES"))
        ;

    }

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

    private void startApplicationContainer() {
        applicationContainer
            .withNetwork(network)
            .withNetworkAliases("application", "application-host")
            .dependsOn(kafkaContainer, postgreSQLContainer)
            .withExposedPorts(8080, 5005)
            .withEnv("JAVA_TOOL_OPTIONS", "-Djava.security.egd=file:/dev/./urandom -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka-host:9092")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/" + postgreSQLContainer.getDatabaseName())
            .withEnv("SPRING_DATASOURCE_USERNAME", postgreSQLContainer.getUsername())
            .withEnv("SPRING_DATASOURCE_PASSWORD", postgreSQLContainer.getPassword())
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"))
            .waitingFor(Wait.forLogMessage(".*Started AlarmServiceMain.*", 1)
            .withStartupTimeout(Duration.ofMinutes(2))
        );
            ;

        // DEBUGGING: uncomment to force local port 5005
        // applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer.start();

        var httpPort = applicationContainer.getMappedPort(8080); // application-http-port
        var debuggerPort = applicationContainer.getMappedPort(5005);

        LOG.info("APPLICATION MAPPED PORTS: http={}; debugger={}",
            httpPort,
            debuggerPort
            );

        System.setProperty("application.base-url", "http://localhost:" + httpPort);
    }
}
