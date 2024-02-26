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

    // Test configuration
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
