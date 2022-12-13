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

package org.opennms.horizon.kafkahelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.opennms.horizon.kafkahelper.internals.KafkaRestCreateConsumerRequest;
import org.opennms.horizon.kafkahelper.internals.KafkaRestSubscribeRequest;
import org.opennms.horizon.kafkahelper.internals.KafkaTestRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class KafkaTestHelper {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(KafkaTestHelper.class);

    private Logger LOG = DEFAULT_LOGGER;

    private RestAssuredConfig restAssuredConfig;
    private String kafkaRestBaseUrl;

    private ObjectMapper objectMapper = new ObjectMapper();

//========================================
// Getters and Setters
//----------------------------------------

    public RestAssuredConfig getRestAssuredConfig() {
        return restAssuredConfig;
    }

    public void setRestAssuredConfig(RestAssuredConfig restAssuredConfig) {
        this.restAssuredConfig = restAssuredConfig;
    }

    public String getKafkaRestBaseUrl() {
        return kafkaRestBaseUrl;
    }

    public void setKafkaRestBaseUrl(String kafkaRestBaseUrl) {
        this.kafkaRestBaseUrl = kafkaRestBaseUrl;
    }

//========================================
// Test Operations
//----------------------------------------

    public void startConsumer(String consumerGroup, String consumerName, String... topics) {
        try {
            createConsumerOnRestServer(consumerGroup, consumerName);
            createSubscriptionsOnRestServer(consumerGroup, consumerName, topics);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void removeConsumer(String consumerGroup, String consumerName) {
        try {
            deleteConsumerOnRestServer(consumerGroup, consumerName);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public List<KafkaTestRecord> getConsumedMessages(String consumerGroup, String consumerName, String topic)  {
        try {
            List<KafkaTestRecord> records = readRecordsFromRestServer(consumerGroup, consumerName);

            List<KafkaTestRecord> result =
                records.stream().filter(record -> Objects.equals(record.getTopic(), topic)).collect(Collectors.toList());

            return result;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

//========================================
// Kafka-Rest Operations
//----------------------------------------

    private void createConsumerOnRestServer(String consumerGroup, String consumerName) throws Exception {
        KafkaRestCreateConsumerRequest kafkaRestCreateConsumerRequest = new KafkaRestCreateConsumerRequest();
        kafkaRestCreateConsumerRequest.setName(consumerName);

        String jsonText = objectMapper.writeValueAsString(kafkaRestCreateConsumerRequest);

        Response response = post("/consumers/" + consumerGroup, jsonText, "application/vnd.kafka.v2+json", null);
        if ((response.getStatusCode() < 200) || (response.getStatusCode() >= 300)) {
            throw new RuntimeException("failed to create consumer: response-code=" + response.getStatusCode() + "; body=" + response.getBody().asString());
        }
    }

    private void createSubscriptionsOnRestServer(String consumerGroup, String consumerName, String[] topics) throws Exception {
        KafkaRestSubscribeRequest kafkaRestSubscribeRequest = new KafkaRestSubscribeRequest();
        kafkaRestSubscribeRequest.setTopics(Arrays.asList(topics));

        String jsonText = objectMapper.writeValueAsString(kafkaRestSubscribeRequest);

        post("/consumers/" + consumerGroup + "/instances/" + consumerName + "/subscription", jsonText, "application/vnd.kafka.v2+json", null);
    }

    private void deleteConsumerOnRestServer(String consumerGroup, String consumerName) throws Exception {
        Response response = delete("/consumers/" + consumerGroup + "/instances/" + consumerName, null);
        if ((response.getStatusCode() < 200) || (response.getStatusCode() >= 300)) {
            throw new RuntimeException("failed to delete consumer: response-code=" + response.getStatusCode() + "; body=" + response.getBody().asString());
        }
    }

    private List<KafkaTestRecord> readRecordsFromRestServer(String consumerGroup, String consumerName) throws Exception {
        Response response =
            get("/consumers/" + consumerGroup + "/instances/" + consumerName + "/records", "application/vnd.kafka.v2+json");

        if ((response.getStatusCode() < 200) || (response.getStatusCode() >= 300)) {
            throw new RuntimeException("failed to read records: response-code=" + response.getStatusCode() + "; body=" + response.getBody().asString());
        }

        KafkaTestRecord[] result = objectMapper.readValue(response.getBody().asString(), KafkaTestRecord[].class);

        return Arrays.asList(result);
    }

//========================================
// REST Operations
//----------------------------------------

    private Response get(String url, String acceptHeader) throws MalformedURLException {
        URL requestUrl = new URL(new URL(kafkaRestBaseUrl), url);

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        if (acceptHeader != null) {
            requestSpecification =
                requestSpecification
                    .header("Accept", acceptHeader)
            ;
        }

        Response response =
            requestSpecification
                .get(requestUrl)
                .thenReturn()
            ;

        return response;
    }

    private Response post(String url, String body, String contentType, String acceptHeader) throws MalformedURLException {
        URL requestUrl = new URL(new URL(kafkaRestBaseUrl), url);

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        if (contentType != null) {
            requestSpecification =
                requestSpecification
                    .header("Content-Type", contentType)
            ;
        }

        if (acceptHeader != null) {
            requestSpecification =
                requestSpecification
                    .header("Accept", acceptHeader)
            ;
        }

        if (body != null) {
            requestSpecification =
                requestSpecification
                    .body(body);
        }

        Response response =
            requestSpecification
                .post(requestUrl)
                .thenReturn()
            ;

        return response;
    }

    private Response delete(String url, String acceptHeader) throws MalformedURLException {
        URL requestUrl = new URL(new URL(kafkaRestBaseUrl), url);

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        if (acceptHeader != null) {
            requestSpecification =
                requestSpecification
                    .header("Accept", acceptHeader)
            ;
        }

        Response response =
            requestSpecification
                .delete(requestUrl)
                .thenReturn()
            ;

        return response;
    }
}
