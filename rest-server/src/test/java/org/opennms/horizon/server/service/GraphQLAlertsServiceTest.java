/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.server.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionProto;
import org.opennms.horizon.alerts.proto.AlertResponse;
import org.opennms.horizon.alerts.proto.DeleteAlertResponse;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.server.model.alerts.AlertEventDefinition;
import org.opennms.horizon.server.model.alerts.TimeRange;
import org.opennms.horizon.server.service.grpc.AlertsClient;
import org.opennms.horizon.server.service.metrics.TSDBMetricsService;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.leangen.graphql.execution.ResolutionEnvironment;

//This purpose of this test class is keep checking the dataloader logic is correct.
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
public class GraphQLAlertsServiceTest {
    private static final String GRAPHQL_PATH = "/graphql";
    @MockBean
    private AlertsClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    @MockBean
    private TSDBMetricsService tsdbMetricsService;
    private final String accessToken = "test-token-12345";
    private Alert alerts1, alerts2;

    private AlertEventDefinition alertEventDefinition1, alertEventDefinition2;
    @Captor
    private ArgumentCaptor<List<DataLoaderFactory.Key>> keyCaptor;

    @BeforeEach
    public void setUp() {
        alerts1 = Alert.newBuilder().setDatabaseId(1).setTenantId("tenant1").setReductionKey("reductionKey1").setSeverity(Severity.CRITICAL).build();
        alerts2 = Alert.newBuilder().setDatabaseId(2).setTenantId("tenant2").setReductionKey("reductionKey2").setSeverity(Severity.CRITICAL).build();
        alertEventDefinition1 = getAlertEventDefinition(1L, "snmpTrap1", "uei1", EventType.SNMP_TRAP, "reductionKey1", "clearKey1");
        alertEventDefinition2 = getAlertEventDefinition(2L, "snmpTrap2", "uei2", EventType.SNMP_TRAP, "reductionKey2", "clearKey2");
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    public void testFindAllAlerts() throws JSONException {
        doReturn(ListAlertsResponse.newBuilder().addAlerts(alerts1).addAlerts(alerts2).build()).when(mockClient).listAlerts(5, 0, Collections.singletonList("CRITICAL"), TimeRange.TODAY, "databaseId", true, "node", accessToken);
        String request = """
            query {
              findAllAlerts(pageSize: 5, page: 0, timeRange: TODAY, severities: ["CRITICAL"], sortBy: "databaseId", sortAscending: true, nodeLabel: "node") {
                nextPage
                alerts {
                  databaseId
                  uei
                  counter
                  severity
                  firstEventTimeMs
                  lastUpdateTimeMs
                  reductionKey
                }
              }
            }""";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findAllAlerts.alerts.size()").isEqualTo(2)
            .jsonPath("$.data.findAllAlerts.alerts[0].severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.findAllAlerts.alerts[0].reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).listAlerts(5, 0, Collections.singletonList("CRITICAL"), TimeRange.TODAY, "databaseId", true, "node", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testAcknowledgeAlert() throws JSONException {
        doReturn(AlertResponse.newBuilder().addAlert(Alert.newBuilder(alerts1).setIsAcknowledged(true).build()).build()).when(mockClient).acknowledgeAlert(List.of(1L), accessToken);
        String request = "mutation {acknowledgeAlert(ids: [\"" + alerts1.getDatabaseId() + "\"]){alertList{severity reductionKey acknowledged}}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.acknowledgeAlert.alertList.size()").isEqualTo(1)
            .jsonPath("$.data.acknowledgeAlert.alertList[0].severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.acknowledgeAlert.alertList[0].acknowledged").isEqualTo(true)
            .jsonPath("$.data.acknowledgeAlert.alertList[0].reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).acknowledgeAlert(List.of(1L), accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testUnacknowledgeAlert() throws JSONException {
        doReturn(AlertResponse.newBuilder().addAlert(Alert.newBuilder(alerts1).setIsAcknowledged(false).build()).build()).when(mockClient).unacknowledgeAlert(List.of(1L), accessToken);
        String request = "mutation {\n" +
            "  unacknowledgeAlert(ids: [\"" + alerts1.getDatabaseId() + "\"]) {\n" +
            "    alertList {\n" +
            "      acknowledged\n" +
            "      ackUser\n" +
            "      severity\n" +
            "      databaseId\n" +
            "    }\n" +
            "    alertErrorList {\n" +
            "      error\n" +
            "      databaseId\n" +
            "    }\n" +
            "  }\n" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.unacknowledgeAlert.alertList.size()").isEqualTo(1)
            .jsonPath("$.data.unacknowledgeAlert.alertList[0].severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.unacknowledgeAlert.alertList[0].acknowledged").isEqualTo(false);
        verify(mockClient).unacknowledgeAlert(List.of(1L), accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testEscalateAlert() throws JSONException {
        doReturn(AlertResponse.newBuilder().addAlert(alerts1).build()).when(mockClient).escalateAlert(List.of(1L), accessToken);
        String request = "mutation {escalateAlert(ids: [\"" + alerts1.getDatabaseId() + "\"]){alertList{reductionKey severity}}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.escalateAlert.alertList.size()").isEqualTo(1)
            .jsonPath("$.data.escalateAlert.alertList[0].severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.escalateAlert.alertList[0].reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).escalateAlert(List.of(1L), accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testClearAlert() throws JSONException {
        doReturn(AlertResponse.newBuilder().addAlert(alerts1).build()).when(mockClient).clearAlert(List.of(1L), accessToken);
        String request = "mutation {clearAlert(ids: [\"" + alerts1.getDatabaseId() + "\"]){alertList{reductionKey severity}}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.clearAlert.alertList.size()").isEqualTo(1)
            .jsonPath("$.data.clearAlert.alertList[0].severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.clearAlert.alertList[0].reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).clearAlert(List.of(1L), accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testDeleteAlert() throws JSONException {
        doReturn(DeleteAlertResponse.newBuilder().addAlertId(1L).build()).when(mockClient).deleteAlert(List.of(1L), accessToken);
        String request = "mutation {deleteAlert(ids: [\"" + alerts1.getDatabaseId() + "\"]){alertDatabaseIdList}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.deleteAlert.alertDatabaseIdList.size()").isEqualTo(1)
            .jsonPath("$.data.deleteAlert.alertDatabaseIdList[0]").isEqualTo(alerts1.getDatabaseId());
        verify(mockClient).deleteAlert(List.of(1L), accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testListAlertEventDefinitions() throws JSONException {
        doReturn(List.of(alertEventDefinition1, alertEventDefinition2))
            .when(mockClient).listAlertEventDefinitions(EventType.SNMP_TRAP, accessToken);
        String request = """
            query {
              listAlertEventDefinitions(eventType: SNMP_TRAP) {
                id
                name
                uei
                eventType
                reductionKey
                clearKey
              }
            }""";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.listAlertEventDefinitions.size()").isEqualTo(2)
            .jsonPath("$.data.listAlertEventDefinitions[0].id").isEqualTo(alertEventDefinition1.getId())
            .jsonPath("$.data.listAlertEventDefinitions[0].uei").isEqualTo(alertEventDefinition1.getUei())
            .jsonPath("$.data.listAlertEventDefinitions[0].name").isEqualTo(alertEventDefinition1.getName())
            .jsonPath("$.data.listAlertEventDefinitions[0].eventType").isEqualTo(alertEventDefinition1.getEventType().name())
            .jsonPath("$.data.listAlertEventDefinitions[0].reductionKey").isEqualTo(alertEventDefinition1.getReductionKey())
            .jsonPath("$.data.listAlertEventDefinitions[0].clearKey").isEqualTo(alertEventDefinition1.getClearKey());
        verify(mockClient).listAlertEventDefinitions(EventType.SNMP_TRAP, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }


    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }

    private AlertEventDefinition getAlertEventDefinition(Long id, String snmpTrap, String uei, EventType eventType, String reductionKey, String clearKey) {
        AlertEventDefinition alertEventDefinition = new AlertEventDefinition();
        alertEventDefinition.setId(id);
        alertEventDefinition.setName(snmpTrap);
        alertEventDefinition.setUei(uei);
        alertEventDefinition.setEventType(eventType);
        alertEventDefinition.setReductionKey(reductionKey);
        alertEventDefinition.setClearKey(clearKey);
        return alertEventDefinition;
    }

}
