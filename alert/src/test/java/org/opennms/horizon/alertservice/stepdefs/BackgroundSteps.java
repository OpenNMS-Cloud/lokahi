/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.alertservice.stepdefs;

import io.cucumber.java.en.Given;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;

@RequiredArgsConstructor
@Slf4j
@Getter
public class BackgroundSteps {

    private final KafkaTestHelper kafkaTestHelper;

    //Test configuration
    private String applicationBaseHttpUrl;
    private String applicationBaseGrpcUrl;
    private String kafkaBootstrapUrl;
    private String eventTopic;
    private String alertTopic;
    private String monitoringPolicyTopic;
    private String tagTopic;

    @Given("Kafka event topic {string}")
    public void createKafkaTopicForEvents(String eventTopic) {
        this.eventTopic = eventTopic;
        kafkaTestHelper.startConsumerAndProducer(eventTopic, eventTopic);
    }

    @Given("Kafka alert topic {string}")
    public void createKafkaTopicForAlerts(String alertTopic) {
        this.alertTopic = alertTopic;
        kafkaTestHelper.startConsumerAndProducer(alertTopic, alertTopic);
    }

    @Given("Kafka monitoring policy topic {string}")
    public void createKafkaTopicForMonitoringPolicy(String monitoringPolicyTopic) {
        this.monitoringPolicyTopic = monitoringPolicyTopic;
        kafkaTestHelper.startConsumerAndProducer(monitoringPolicyTopic, monitoringPolicyTopic);
    }

    @Given("Kafka tag topic {string}")
    public void createKafkaTopicForTags(String tagTopic) {
        this.tagTopic = tagTopic;
        kafkaTestHelper.startConsumerAndProducer(tagTopic, tagTopic);
    }

    @Given("Application base HTTP URL in system property {string}")
    public void applicationBaseHttpUrlInSystemProperty(String systemProperty) {
        this.applicationBaseHttpUrl = System.getProperty(systemProperty);

        log.info("Using base HTTP URL {}", this.applicationBaseHttpUrl);
    }

    @Given("Application base gRPC URL in system property {string}")
    public void applicationBaseGrpcUrlInSystemProperty(String systemProperty) {
        this.applicationBaseGrpcUrl = System.getProperty(systemProperty);

        log.info("Using base gRPC URL: {}", this.applicationBaseGrpcUrl);
    }

    @Given("Kafka bootstrap URL in system property {string}")
    public void kafkaRestServerURLInSystemProperty(String systemProperty) {
        this.kafkaBootstrapUrl = System.getProperty(systemProperty);
        log.info("Using Kafka base URL: {}", this.kafkaBootstrapUrl);
        kafkaTestHelper.setKafkaBootstrapUrl(kafkaBootstrapUrl);
    }
}
