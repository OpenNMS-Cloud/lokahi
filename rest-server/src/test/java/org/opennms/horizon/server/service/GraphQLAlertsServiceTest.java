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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.model.common.proto.Severity;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.config.DataLoaderFactory;
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
    @Captor
    private ArgumentCaptor<List<DataLoaderFactory.Key>> keyCaptor;

    @BeforeEach
    public void setUp() {
        alerts1 = Alert.newBuilder().setDatabaseId(1).setTenantId("tenant1").setReductionKey("reductionKey1").setSeverity(Severity.CRITICAL).build();
        alerts2 = Alert.newBuilder().setDatabaseId(2).setTenantId("tenant2").setReductionKey("reductionKey2").setSeverity(Severity.CRITICAL).build();
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    public void testFindAllAlerts() throws JSONException {
        doReturn(Arrays.asList(alerts1, alerts2)).when(mockClient).listAlerts(5, "0", accessToken);
        String request = "query {findAllAlerts(pageSize:5, page: \"0\") {tenantId reductionKey severity}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findAllAlerts.size()").isEqualTo(2)
            .jsonPath("$.data.findAllAlerts[0].tenantId").isEqualTo(alerts1.getTenantId())
            .jsonPath("$.data.findAllAlerts[0].severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.findAllAlerts[0].reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).listAlerts(5, "0", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testAcknowledgeAlert() throws JSONException {
        doReturn(alerts1).when(mockClient).acknowledgeAlert(1, accessToken);
        String request = "mutation {acknowledgeAlert(id: \"" + alerts1.getDatabaseId() + "\"){tenantId reductionKey severity}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.size()").isEqualTo(1)
            .jsonPath("$.data.acknowledgeAlert.tenantId").isEqualTo(alerts1.getTenantId())
            .jsonPath("$.data.acknowledgeAlert.severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.acknowledgeAlert.reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).acknowledgeAlert(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testUnacknowledgeAlert() throws JSONException {
        doReturn(alerts1).when(mockClient).unacknowledgeAlert(1, accessToken);
        String request = "mutation {unacknowledgeAlert(id: \"" + alerts1.getDatabaseId() + "\"){tenantId reductionKey severity}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.size()").isEqualTo(1)
            .jsonPath("$.data.unacknowledgeAlert.tenantId").isEqualTo(alerts1.getTenantId())
            .jsonPath("$.data.unacknowledgeAlert.severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.unacknowledgeAlert.reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).unacknowledgeAlert(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testEscalateAlert() throws JSONException {
        doReturn(alerts1).when(mockClient).escalateAlert(1, accessToken);
        String request = "mutation {escalateAlert(id: \"" + alerts1.getDatabaseId() + "\"){tenantId reductionKey severity}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.size()").isEqualTo(1)
            .jsonPath("$.data.escalateAlert.tenantId").isEqualTo(alerts1.getTenantId())
            .jsonPath("$.data.escalateAlert.severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.escalateAlert.reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).escalateAlert(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testClearAlert() throws JSONException {
        doReturn(alerts1).when(mockClient).clearAlert(1, accessToken);
        String request = "mutation {clearAlert(id: \"" + alerts1.getDatabaseId() + "\"){tenantId reductionKey severity}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.size()").isEqualTo(1)
            .jsonPath("$.data.clearAlert.tenantId").isEqualTo(alerts1.getTenantId())
            .jsonPath("$.data.clearAlert.severity").isEqualTo(alerts1.getSeverity().name())
            .jsonPath("$.data.clearAlert.reductionKey").isEqualTo(alerts1.getReductionKey());
        verify(mockClient).clearAlert(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testDeleteAlert() throws JSONException {
        doReturn(true).when(mockClient).deleteAlert(1, accessToken);
        String request = "mutation {deleteAlert(id: \"" + alerts1.getDatabaseId() + "\")}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.deleteAlert").isEqualTo(true);
        verify(mockClient).deleteAlert(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }

}
