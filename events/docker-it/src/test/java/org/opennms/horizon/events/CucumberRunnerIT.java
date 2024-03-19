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
package org.opennms.horizon.events;

import static io.cucumber.core.options.Constants.*;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import java.time.Duration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("org/opennms/horizon/events/")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "json:target/cucumber-report.json, html:target/cucumber.html, pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.opennms.horizon.events")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @ignore")
public class CucumberRunnerIT {
    private static final Logger LOG = LoggerFactory.getLogger(CucumberRunnerIT.class);
    public static final String KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME = "kafka.bootstrap-servers";
    private static final String confluentPlatformVersion = "7.3.0";
    private static GenericContainer applicationContainer;
    private static PostgreSQLContainer postgreSQLContainer;
    private static KafkaContainer kafkaContainer;
    private static Network network;
    private static final String dockerImage = System.getProperty("application.docker.image");

    @BeforeAll
    @SuppressWarnings({"unchecked"})
    public static void before() throws Throwable {
        network = Network.newNetwork();

        kafkaContainer = new KafkaContainer(
                        DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion))
                .withNetwork(network)
                .withNetworkAliases("kafka", "kafka:host")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("KAFKA"));

        postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.5-alpine")
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withDatabaseName("events")
                .withUsername("events")
                .withPassword("password")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("POSTGRES"));

        kafkaContainer.start();
        postgreSQLContainer.start();

        String bootstrapServers = kafkaContainer.getBootstrapServers();
        System.setProperty(KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME, bootstrapServers);
        LOG.info("KAFKA LOCALHOST BOOTSTRAP SERVERS {}", bootstrapServers);

        startApplicationContainer(
                false); // DEBUGGING - set to true to expose the application debugging on host port 5005
    }

    @AfterAll
    public static void shutdown() {
        applicationContainer.stop();
        kafkaContainer.stop();
        postgreSQLContainer.stop();
    }

    @SuppressWarnings({"unchecked"})
    private static void startApplicationContainer(boolean enableDebuggingPort5005) {
        var jdbcUrl =
                "jdbc:postgresql://" + postgreSQLContainer.getNetworkAliases().get(0) + ":5432" + "/"
                        + postgreSQLContainer.getDatabaseName();

        applicationContainer =
                new GenericContainer(DockerImageName.parse(dockerImage).toString());
        applicationContainer
                .withNetwork(network)
                .withNetworkAliases("application", "application-host")
                .dependsOn(kafkaContainer, postgreSQLContainer)
                .withStartupTimeout(Duration.ofMinutes(5))
                .withEnv(
                        "JAVA_TOOL_OPTIONS",
                        "-Djava.security.egd=file:/dev/./urandom -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
                .withEnv(
                        "SPRING_KAFKA_BOOTSTRAP_SERVERS",
                        kafkaContainer.getNetworkAliases().get(0) + ":9092")
                .withEnv("SPRING_DATASOURCE_URL", jdbcUrl)
                .withEnv("SPRING_DATASOURCE_USERNAME", postgreSQLContainer.getUsername())
                .withEnv("SPRING_DATASOURCE_PASSWORD", postgreSQLContainer.getPassword())
                .withEnv("INVENTORY_ENCRYPTION_KEY", RandomStringUtils.randomAlphanumeric(32))
                // Uncomment to get Hibernate SQL logging
                // .withEnv("logging.level.org.hibernate.SQL", "DEBUG")
                // .withEnv("logging.level.org.hibernate.orm.jdbc.bind", "TRACE")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"));

        if (!enableDebuggingPort5005) {
            applicationContainer.withExposedPorts(6565, 8080, 5005);
        } else {
            applicationContainer.withExposedPorts(6565, 8080);

            // DEBUGGING: uncomment to force local port 5005 (also comment-out the 5005 in withExposedPorts() above
            applicationContainer.getPortBindings().add("5005:5005");
        }

        applicationContainer.start();

        var externalGrpcPort = applicationContainer.getMappedPort(6565); // application-external-grpc-port
        var externalHttpPort = applicationContainer.getMappedPort(8080);
        var debuggerPort = applicationContainer.getMappedPort(5005);
        LOG.info(
                "APPLICATION MAPPED PORTS:  external-grpc={};  external-http={}; debugger={}",
                externalGrpcPort,
                externalHttpPort,
                debuggerPort);
        System.setProperty("application-external-grpc-port", String.valueOf(externalGrpcPort));
        System.setProperty("application-external-http-port", String.valueOf(externalHttpPort));
        System.setProperty("application-external-http-base-url", "http://localhost:" + externalHttpPort);
    }
}
