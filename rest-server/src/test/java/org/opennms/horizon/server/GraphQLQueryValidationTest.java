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
package org.opennms.horizon.server;

import static org.opennms.horizon.server.test.util.ResourceFileReader.read;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opennms.horizon.server.test.config.GraphQLQueryValidationConfig;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@Slf4j
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "logging.level.org.opennms.horizon.server.service.graphql.DuplicateFieldValidation=DEBUG",
            "logging.level.org.opennms.horizon.server.service.graphql.MaxAliasOccurrenceValidation=DEBUG",
            "logging.level.org.opennms.horizon.server.service.graphql.MaxComplexityInstrumentation=DEBUG",
            "logging.level.org.opennms.horizon.server.service.graphql.MaxDepthInstrumentation=DEBUG",
            "logging.level.org.opennms.horizon.server.service.graphql.MaxDirectiveOccurrenceInstrumentation=DEBUG"
        })
@Import(GraphQLQueryValidationConfig.class)
public class GraphQLQueryValidationTest {
    private static Arguments readArgumentPair(String filePath) {
        return Arguments.of(filePath, read(filePath));
    }

    private static final String DESCRIPTIVE_DISPLAY_NAME = "[{index}]: {0}";

    private GraphQLWebTestClient webClient;

    @BeforeEach
    void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
    }

    public static Stream<Arguments> allowedRequests() {
        return Stream.of(
                readArgumentPair("/test/data/list-monitoring-policies.json"),
                readArgumentPair("/test/data/mutation-create-location.json"),
                readArgumentPair("/test/data/mutation-create-location-2.json"),
                readArgumentPair("/test/data/mutation-create-monitor-policy.json"),
                readArgumentPair("/test/data/mutation-create-or-update-active-icmp-discovery.json"),
                readArgumentPair("/test/data/query-alerts-list.json"),
                readArgumentPair("/test/data/query-build-network-inventory-page.json"),
                readArgumentPair("/test/data/query-count-alerts.json"),
                readArgumentPair("/test/data/query-find-application-summaries.json"),
                readArgumentPair("/test/data/query-find-exporters-for-node-status.json"),
                readArgumentPair("/test/data/query-find-exporters.json"),
                readArgumentPair("/test/data/query-find-minions-by-location-id.json"),
                readArgumentPair("/test/data/query-get-minion-certificate.json"),
                readArgumentPair("/test/data/query-get-minion-certificate.json"),
                readArgumentPair("/test/data/query-list-alert-event-definitions.json"),
                readArgumentPair("/test/data/query-list-discoveries-passive-discoveries.json"),
                readArgumentPair("/test/data/query-list-locations-for-discovery.json"),
                readArgumentPair("/test/data/query-list-minions-for-table-minions-table.json"),
                readArgumentPair("/test/data/query-list-monitory-policies.json"),
                readArgumentPair("/test/data/query-list-node-status.json"),
                readArgumentPair("/test/data/query-list-tags-search.json"),
                readArgumentPair("/test/data/query-location-by-name.json"),
                readArgumentPair("/test/data/query-locations-list.json"),
                readArgumentPair("/test/data/query-network-traffic.json"),
                readArgumentPair("/test/data/query-node-id.json"),
                readArgumentPair("/test/data/query-nodes-for-map-find-all-nodes.json"),
                readArgumentPair("/test/data/query-search-location.json"),
                readArgumentPair("/test/data/query-tags-by-active-discovery-id.json"),
                readArgumentPair("/test/data/query-tags-by-passive-discovery-id.json"),
                readArgumentPair("/test/data/mutation-update-node.json"));
    }

    @MethodSource("allowedRequests")
    @ParameterizedTest(name = DESCRIPTIVE_DISPLAY_NAME)
    void allowedRequestsPassValidation(String description, String requestBody) {
        webClient.exchangePost(requestBody).expectCleanResponse();
    }

    public static Stream<Arguments> introspectionRequests() {
        return Stream.of(
                readArgumentPair("/test/data/simple-introspection-query.json"),
                readArgumentPair("/test/data/introspection-query.json"),
                readArgumentPair("/test/data/introspection-circular.json"));
    }

    @MethodSource("introspectionRequests")
    @ParameterizedTest(name = DESCRIPTIVE_DISPLAY_NAME)
    void disallowsIntrospection(String description, String requestBody) {
        webClient
                .exchangePost(requestBody)
                .expectGraphQLErrorResponse()
                .jsonPath("$.errors[*].message")
                .value(Matchers.everyItem(
                        Matchers.matchesRegex("^Validation error.*: Field '.+' in type '__.+' is undefined$")));
    }

    @Test
    void wellFormedButInvalidRequestShouldReturn200WithErrorDetails() {
        String requestBody = read("/test/data/error-mishandling.json");

        webClient.exchangePost(requestBody).expectGraphQLErrorResponse();
    }

    @Test
    void limitsAliasOverloading() {
        String requestBody = read("/test/data/alias-overloading.json");

        webClient
                .exchangePost(requestBody)
                .expectGraphQLErrorResponse()
                .jsonPath("$.errors[*].message")
                .value(Matchers.everyItem(
                        Matchers.matchesRegex("^Validation error.*: Alias '.+' is repeated too many times$")))
                .jsonPath("$.trace")
                .doesNotExist();
    }

    public static Stream<Arguments> fieldDuplicationRequests() {
        return Stream.of(
                readArgumentPair("/test/data/field-duplication.json"),
                readArgumentPair("/test/data/notifyByEmail-duplication.json"));
    }

    @MethodSource("fieldDuplicationRequests")
    @ParameterizedTest(name = DESCRIPTIVE_DISPLAY_NAME)
    void limitsFieldDuplication(String description, String requestBody) {
        webClient
                .exchangePost(requestBody)
                .expectGraphQLErrorResponse()
                .jsonPath("$.errors[*].message")
                .value(Matchers.everyItem(
                        Matchers.matchesRegex("^Validation error.*: Field '.+' is repeated too many times$")))
                .jsonPath("$.trace")
                .doesNotExist();
    }

    @Test
    void limitsDirectiveOverloading() {
        String requestBody = read("/test/data/directive-overloading.json");

        webClient
                .exchangePost(requestBody)
                .expectGraphQLErrorResponse()
                .jsonPath("$.errors[*].message")
                .value(Matchers.everyItem(
                        Matchers.matchesRegex("^Validation error.*: Directive '.+' is repeated too many times$")))
                .jsonPath("$.trace")
                .doesNotExist();
    }
}
