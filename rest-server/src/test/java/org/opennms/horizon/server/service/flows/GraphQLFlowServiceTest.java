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
package org.opennms.horizon.server.service.flows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.util.List;
import org.json.JSONException;
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
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLFlowServiceTest {
    private final String tenantId = "tenantId";

    @MockBean(name = "flowQuerier")
    private ManagedChannel channel;

    @MockBean
    private FlowClient mockFlowClient;

    @MockBean
    private InventoryClient mockInventoryClient;

    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private GraphQLWebTestClient webClient;
    private String accessToken;
    private IpInterfaceDTO ipInterfaceDTO = IpInterfaceDTO.newBuilder()
            .setId(1L)
            .setNodeId(1L)
            .setIpAddress("127.0.0.1")
            .setHostname("localhost")
            .setSnmpPrimary(true)
            .setSnmpInterfaceId(2)
            .build();
    private NodeDTO nodeDTO = NodeDTO.newBuilder()
            .setId(1L)
            .setNodeLabel("label")
            .addSnmpInterfaces(SnmpInterfaceDTO.newBuilder()
                    .setId(1)
                    .setIfIndex(1)
                    .setIfName("eth0")
                    .setIfAdminStatus(1)
                    .setIfOperatorStatus(1))
            .addSnmpInterfaces(SnmpInterfaceDTO.newBuilder()
                    .setId(2)
                    .setIfIndex(2)
                    .setIfName("eth1")
                    .setIfAdminStatus(1)
                    .setIfOperatorStatus(1))
            .build();

    @BeforeEach
    public void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
        accessToken = webClient.getAccessToken();

        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any());
        doReturn(tenantId).when(mockHeaderUtil).extractTenant(any());
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
        when(mockInventoryClient.getIpInterfaceById(anyLong(), anyString())).thenAnswer(i -> {
            if (i.getArgument(0).equals(ipInterfaceDTO.getId())) {
                return ipInterfaceDTO;
            } else {
                throw new StatusRuntimeException(Status.NOT_FOUND);
            }
        });
        doReturn(nodeDTO).when(mockInventoryClient).getNodeById(anyLong(), anyString());
    }

    @Test
    void testFindExporters() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        // id = 2 is invalid
        doReturn(List.of(1L, 2L))
                .when(mockFlowClient)
                .findExporters(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request =
                """
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
                snmpInterface {
                  ifName
                  ifIndex
                }
              }
            }
            """;

        var matchedSnmpInterface = nodeDTO.getSnmpInterfacesList().stream()
                .filter(s -> s.getId() == ipInterfaceDTO.getSnmpInterfaceId())
                .findFirst();
        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                // id = 2 is expected to skip silently
                .jsonPath("$.data.findExporters.size()")
                .isEqualTo(1)
                .jsonPath("$.data.findExporters[0].node.nodeLabel")
                .isEqualTo(nodeDTO.getNodeLabel())
                .jsonPath("$.data.findExporters[0].ipInterface.ipAddress")
                .isEqualTo(ipInterfaceDTO.getIpAddress())
                .jsonPath("$.data.findExporters[0].snmpInterface.ifName")
                .isEqualTo(matchedSnmpInterface.get().getIfName())
                .jsonPath("$.data.findExporters[0].snmpInterface.ifIndex")
                .isEqualTo(matchedSnmpInterface.get().getIfIndex());
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }

    @Test
    void testFindApplications() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        List<String> applications = List.of("http", "mysql");
        doReturn(applications)
                .when(mockFlowClient)
                .findApplications(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request =
                """
            query {
              findApplications(
                requestCriteria: {
                  timeRange: { startTime: 1681177700000, endTime: 1681277720000 }
                }
              )
            }
            """;

        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.findApplications.size()")
                .isEqualTo(applications.size())
                .jsonPath("$.data.findApplications[0]")
                .isEqualTo(applications.get(0));
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }

    @Test
    void testFindApplicationSummaries() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        Summaries summaries = Summaries.newBuilder()
                .addSummaries(TrafficSummary.newBuilder()
                        .setApplication("http")
                        .setBytesIn(10)
                        .setBytesOut(20))
                .build();
        doReturn(summaries)
                .when(mockFlowClient)
                .getApplicationSummaries(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request =
                """
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

        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.findApplicationSummaries.size()")
                .isEqualTo(summaries.getSummariesCount())
                .jsonPath("$.data.findApplicationSummaries[0].label")
                .isEqualTo(summaries.getSummaries(0).getApplication());
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }

    @Test
    void testFindApplicationSeries() throws JSONException {
        ArgumentCaptor<String> tenantIdArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> accessTokenArg = ArgumentCaptor.forClass(String.class);
        Series flowingPoints = Series.newBuilder()
                .addPoint(FlowingPoint.newBuilder()
                        .setApplication("http")
                        .setDirection(Direction.INGRESS)
                        .setValue(100L)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis())))
                .addPoint(FlowingPoint.newBuilder()
                        .setApplication("http")
                        .setDirection(Direction.INGRESS)
                        .setValue(100L)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() + 1)))
                .build();
        doReturn(flowingPoints)
                .when(mockFlowClient)
                .getApplicationSeries(any(RequestCriteria.class), tenantIdArg.capture(), accessTokenArg.capture());

        String request =
                """
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

        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.findApplicationSeries.size()")
                .isEqualTo(flowingPoints.getPointCount())
                .jsonPath("$.data.findApplicationSeries[0].label")
                .isEqualTo(flowingPoints.getPoint(0).getApplication());
        assertEquals(tenantId, tenantIdArg.getValue());
        assertEquals(accessToken, accessTokenArg.getValue());
    }
}
