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
package org.opennms.horizon.testcontainers;

import java.time.Duration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

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
        kafkaContainer = new KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion));
        postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.5-alpine");
        applicationContainer =
                new GenericContainer(DockerImageName.parse(dockerImage).toString());
    }

    @Override
    protected void before() throws Throwable {
        network = Network.newNetwork();

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

    // ========================================
    // Container Startups
    // ----------------------------------------

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
                .withEnv(
                        "JAVA_TOOL_OPTIONS",
                        "-Djava.security.egd=file:/dev/./urandom -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka-host:9092")
                .withEnv("IGNITE_USE_KUBERNETES", "false")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"));

        // DEBUGGING: uncomment to force local port 5005, and remove 5005 from .withExposedPorts() above
        // applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer.start();

        var httpPort = applicationContainer.getMappedPort(8080); // application-http-port
        var externalGrpcPort = applicationContainer.getMappedPort(8990); // application-external-grpc-port
        var internalGrpcPort = applicationContainer.getMappedPort(8991); // application-internal-grpc-port

        LOG.info(
                "APPLICATION MAPPED PORTS: http={}; external-grpc={}; internal-grpc={};",
                httpPort,
                externalGrpcPort,
                internalGrpcPort);

        System.setProperty("application.base-url", "http://localhost:" + httpPort);
        System.setProperty("application-external-grpc-port", String.valueOf(externalGrpcPort));
        System.setProperty("application-internal-grpc-port", String.valueOf(internalGrpcPort));
    }
}
