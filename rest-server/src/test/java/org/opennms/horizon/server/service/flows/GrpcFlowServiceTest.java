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

package org.opennms.horizon.server.service.flows;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.leangen.graphql.execution.ResolutionEnvironment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.dataplatform.flows.querier.v1.Direction;
import org.opennms.dataplatform.flows.querier.v1.FlowingPoint;
import org.opennms.dataplatform.flows.querier.v1.Series;
import org.opennms.dataplatform.flows.querier.v1.Summaries;
import org.opennms.dataplatform.flows.querier.v1.TrafficSummary;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GrpcFlowServiceTest {
    private static final String GRAPHQL_PATH = "/graphql";
    private final String tenantId = "tenantId";
    private final String accessToken = "accessToken";

    @Autowired
    private WebTestClient webClient;

    @MockBean(name = "flowQuerier")
    private ManagedChannel channel;
    @MockBean
    private FlowClient mockFlowClient;
    @MockBean
    private InventoryClient mockInventoryClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    private IpInterfaceDTO ipInterfaceDTO = IpInterfaceDTO.newBuilder()
        .setId(1L).setNodeId(1L).setIpAddress("127.0.0.1").setHostname("localhost").setSnmpPrimary(true).build();
    private NodeDTO nodeDTO = NodeDTO.newBuilder().setId(1L).setNodeLabel("label").build();

    @BeforeEach
    public void setUp() {
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any());
        doReturn(tenantId).when(mockHeaderUtil).extractTenant(any());
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
        doReturn(ipInterfaceDTO).when(mockInventoryClient).getIpInterfaceById(anyLong(), anyString());
        doReturn(nodeDTO).when(mockInventoryClient).getNodeById(anyLong(), anyString());
    }

    @Test
    void testFindExporters() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        doReturn(List.of(1L)).when(mockFlowClient)
            .findExporters(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request = """
            query {
              findExporters(
                requestCriteria: {
                  timeRange: { startTime: 1680479213000, endTime: 1680822000000 }
                }
              ) {
                node {
                  id
                  nodeLabel
                }
                ipInterface {
                  id
                  ipAddress
                  hostname
                  snmpPrimary
                }
              }
            }
            """;

        webClient.post().uri(GRAPHQL_PATH).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request)).exchange().expectStatus().isOk().expectBody()
            .jsonPath("$.data.findExporters.size()").isEqualTo(1)
            .jsonPath("$.data.findExporters[0].node.nodeLabel").isEqualTo(nodeDTO.getNodeLabel())
            .jsonPath("$.data.findExporters[0].ipInterface.ipAddress").isEqualTo(ipInterfaceDTO.getIpAddress());
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }

    @Test
    void testFindApplications() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        List<String> applications = List.of("http", "mysql");
        doReturn(applications).when(mockFlowClient)
            .findApplications(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request = """
            query {
              findApplications(
                requestCriteria: {
                  timeRange: { startTime: 1681177700000, endTime: 1681277720000 }
                }
              )
            }
            """;

        webClient.post().uri(GRAPHQL_PATH).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request)).exchange().expectStatus().isOk().expectBody()
            .jsonPath("$.data.findApplications.size()").isEqualTo(applications.size())
            .jsonPath("$.data.findApplications[0]").isEqualTo(applications.get(0));
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }

    @Test
    void testFindApplicationSummaries() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        Summaries summaries = Summaries.newBuilder().addSummaries(TrafficSummary.newBuilder()
            .setApplication("http").setBytesIn(10).setBytesOut(20)).build();
        doReturn(summaries).when(mockFlowClient)
            .getApplicationSummaries(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request = """
            query {
              findApplicationSummaries(
                requestCriteria: {
                  timeRange: { startTime: 1681177700000, endTime: 1681277720000 }
                }
              ) {
                label
                bytesIn
                bytesOut
              }
            }
            """;

        webClient.post().uri(GRAPHQL_PATH).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request)).exchange().expectStatus().isOk().expectBody()
            .jsonPath("$.data.findApplicationSummaries.size()").isEqualTo(summaries.getSummariesCount())
            .jsonPath("$.data.findApplicationSummaries[0].label").isEqualTo(summaries.getSummaries(0).getApplication());
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }

    @Test
    void testFindApplicationSeries() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        Series flowingPoints = Series.newBuilder().addPoint(FlowingPoint.newBuilder()
            .setApplication("http").setDirection(Direction.INGRESS).setValue(10).setTimestamp(Timestamp.newBuilder()))
            .build();
        doReturn(flowingPoints).when(mockFlowClient)
            .getApplicationSeries(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request = """
            query {
              findApplicationSeries(
                requestCriteria: {
                  timeRange: { startTime: 1681177700000, endTime: 1681277720000 }
                }
              ) {
                  timestamp
                  label
                  value
                  direction
              }
            }
            """;

        webClient.post().uri(GRAPHQL_PATH).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request)).exchange().expectStatus().isOk().expectBody()
            .jsonPath("$.data.findApplicationSeries.size()").isEqualTo(flowingPoints.getPointCount())
            .jsonPath("$.data.findApplicationSeries[0].label").isEqualTo(flowingPoints.getPoint(0).getApplication());
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
