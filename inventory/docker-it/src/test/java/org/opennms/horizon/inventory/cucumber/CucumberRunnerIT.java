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

package org.opennms.horizon.inventory.cucumber;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static io.cucumber.core.options.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("org/opennms/horizon/inventory/")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "json:target/cucumber-report.json, html:target/cucumber.html, pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.opennms.horizon.inventory,org.opennms.horizon.testtool.miniongateway.wiremock.client")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @ignore")
public class CucumberRunnerIT {

    public static final String KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME = "kafka.bootstrap-servers";

    private static final Logger LOG = LoggerFactory.getLogger(CucumberRunnerIT.class);

    private static final String confluentPlatformVersion = "7.3.0";
    private static final String wireMockVersion = "2.35.0";

    private static KafkaContainer kafkaContainer;
    private static GenericContainer applicationContainer;
    private static GenericContainer azureWireMockContainer;
    private static GenericContainer<?> mockAlertContainer;
    private static PostgreSQLContainer postgreSQLContainer;

    private static Network network;

    private static final String dockerImage = System.getProperty("application.docker.image");

    @BeforeAll
    @SuppressWarnings({"unchecked"})
    public static void before() throws Throwable {
        network = Network.newNetwork();
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion))
            .withNetwork(network)
            .withNetworkAliases("kafka", "kafka:host")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("KAFKA"))
        ;

        postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.5-alpine")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("inventory").withUsername("inventory")
            .withPassword("password")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("POSTGRES"))
        ;

        azureWireMockContainer = new GenericContainer(DockerImageName.parse("wiremock/wiremock").withTag(wireMockVersion))
            .withNetwork(network)
            .withNetworkAliases("wiremock")
            .withExposedPorts(8080)
            .withClasspathResourceMapping("wiremock", "/home/wiremock/mappings", BindMode.READ_ONLY)
            .withCommand("--global-response-templating", "--disable-banner", "--verbose")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("WIREMOCK"));

        String alertHost = "opennms-alert";
        int alertPort = 4770;
        mockAlertContainer = new GenericContainer<>(DockerImageName.parse("tkpd/gripmock"))
            .withNetwork(network)
            .withNetworkAliases(alertHost)
            .withExposedPorts(alertPort)
            .withClasspathResourceMapping("proto", "/proto", BindMode.READ_ONLY)
            // refer from jar file
            .withClasspathResourceMapping("alerts.proto", "/proto/alerts.proto", BindMode.READ_ONLY)
            // make sure stub file don't have tail 0A return char
            .withCommand("--stub=/proto/stub", "/proto/alerts.proto")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("MOCK"))
            .waitingFor(Wait.forListeningPort()
                .withStartupTimeout(Duration.ofSeconds(10))
            );

        kafkaContainer.start();
        postgreSQLContainer.start();
        azureWireMockContainer.start();
        mockAlertContainer.start();

        String bootstrapServers = kafkaContainer.getBootstrapServers();
        System.setProperty(KAFKA_BOOTSTRAP_SERVER_PROPERTYNAME, bootstrapServers);
        LOG.info("KAFKA LOCALHOST BOOTSTRAP SERVERS {}", bootstrapServers);
        
        startApplicationContainer(false, alertHost + ":" + alertPort);   // DEBUGGING - set to true to expose the application debugging on host port 5005
    }

    @AfterAll
    public static void shutdown() {
        applicationContainer.stop();
        kafkaContainer.stop();
        azureWireMockContainer.stop();
        mockAlertContainer.stop();
        postgreSQLContainer.stop();
    }

    @SuppressWarnings({"unchecked"})
    private static void startApplicationContainer(boolean enableDebuggingPort5005, String alertContainerUrl) {
        var azureWiremockUrl = "http://" + azureWireMockContainer.getNetworkAliases().get(0) + ":8080";
        var jdbcUrl = "jdbc:postgresql://" + postgreSQLContainer.getNetworkAliases().get(0) + ":5432" + "/" + postgreSQLContainer.getDatabaseName();

        applicationContainer = new GenericContainer(DockerImageName.parse(dockerImage).toString());
        applicationContainer
            .withNetwork(network)
            .withNetworkAliases("application", "application-host")
            .dependsOn(kafkaContainer, azureWireMockContainer, postgreSQLContainer)
            .withStartupTimeout(Duration.ofMinutes(5))
            .withEnv("JAVA_TOOL_OPTIONS", "-Djava.security.egd=file:/dev/./urandom -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", kafkaContainer.getNetworkAliases().get(0) + ":9092")
            .withEnv("SPRING_DATASOURCE_URL", jdbcUrl)
            .withEnv("SPRING_DATASOURCE_USERNAME", postgreSQLContainer.getUsername())
            .withEnv("SPRING_DATASOURCE_PASSWORD", postgreSQLContainer.getPassword())
            .withEnv("INVENTORY_AZURE_LOGIN_URL", azureWiremockUrl)
            .withEnv("INVENTORY_AZURE_MANAGEMENT_URL", azureWiremockUrl)
            .withEnv("INVENTORY_ENCRYPTION_KEY", RandomStringUtils.randomAlphanumeric(32))
            .withEnv("grpc.client.alert.url", alertContainerUrl)
            // Uncomment to get Hibernate SQL logging
            // .withEnv("logging.level.org.hibernate.SQL", "DEBUG")
            // .withEnv("logging.level.org.hibernate.orm.jdbc.bind", "TRACE")
            .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("APPLICATION"));

        if (! enableDebuggingPort5005) {
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
        LOG.info("APPLICATION MAPPED PORTS:  external-grpc={};  external-http={}; debugger={}", externalGrpcPort, externalHttpPort, debuggerPort);
        System.setProperty("application-external-grpc-port", String.valueOf(externalGrpcPort));
        System.setProperty("application-external-http-port", String.valueOf(externalHttpPort));
        System.setProperty("application-external-http-base-url", "http://localhost:" + externalHttpPort);
    }

}
