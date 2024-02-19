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
package org.opennms.horizon.alertservice.testcontainers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.junit.rules.ExternalResource;
import org.keycloak.common.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings({"rawtypes", "unchecked"})
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

    private KeyPair jwtKeyPair;

    public TestContainerRunnerClassRule() {
        kafkaContainer = new KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion));
        applicationContainer = new GenericContainer(DockerImageName.parse(dockerImage));
        postgreSQLContainer = new PostgreSQLContainer("postgres:14.5-alpine");
    }

    @Override
    protected void before() {
        network = Network.newNetwork();
        LOG.info("USING TEST DOCKER NETWORK {}", network.getId());

        try {
            jwtKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

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

    // ========================================
    // Container Startups
    // ----------------------------------------

    private void startPostgresContainer() {
        postgreSQLContainer
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withEnv("POSTGRESS_CLIENT_PORT", String.valueOf(PostgreSQLContainer.POSTGRESQL_PORT))
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("POSTGRES"));
        // DEBUGGING: uncomment to interact/view DB
        // postgreSQLContainer.getPortBindings().add("5432:5432");
    }

    private void startKafkaContainer() {
        kafkaContainer
                .withEmbeddedZookeeper()
                .withNetwork(network)
                .withNetworkAliases("kafka", "kafka-host")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("KAFKA"))
                .start();

        String bootstrapServers = kafkaContainer.getBootstrapServers();
        System.setProperty(KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME, bootstrapServers);
        LOG.info("KAFKA LOCALHOST BOOTSTRAP SERVERS {}", bootstrapServers);
    }

    private void startApplicationContainer() {
        applicationContainer
                .withNetwork(network)
                .withNetworkAliases("application", "application-host")
                .dependsOn(kafkaContainer, postgreSQLContainer)
                .withExposedPorts(8080, 6565, 5005)
                .withEnv("JAVA_TOOL_OPTIONS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka-host:9092")
                .withEnv(
                        "SPRING_DATASOURCE_URL",
                        "jdbc:postgresql://postgres:5432/" + postgreSQLContainer.getDatabaseName())
                .withEnv("SPRING_DATASOURCE_USERNAME", postgreSQLContainer.getUsername())
                .withEnv("SPRING_DATASOURCE_PASSWORD", postgreSQLContainer.getPassword())
                .withEnv(
                        "KEYCLOAK_PUBLIC_KEY",
                        Base64.encodeBytes(jwtKeyPair.getPublic().getEncoded()))
                .withEnv("SPRING_LIQUIBASE_CONTEXTS", "test")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"))
                .waitingFor(Wait.forLogMessage(".*Started AlertServiceMain.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));

        // DEBUGGING: uncomment to force local port 5005
        // applicationContainer.getPortBindings().add("5005:5005");
        applicationContainer.start();

        var httpPort = applicationContainer.getMappedPort(8080); // application-http-port
        var grpcPort = applicationContainer.getMappedPort(6565); // application-grpc-port
        var debuggerPort = applicationContainer.getMappedPort(5005);

        LOG.info("APPLICATION MAPPED PORTS: http={}, grpc={}; debugger={}", httpPort, grpcPort, debuggerPort);
        System.setProperty("application.base-http-url", "http://localhost:" + httpPort);
        System.setProperty("application.base-grpc-url", "http://localhost:" + grpcPort);
    }

    public KeyPair getJwtKeyPair() {
        return jwtKeyPair;
    }

    public int getGrpcPort() {
        return applicationContainer.getMappedPort(6565);
    }
}
