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
package org.opennms.horizon.server.service;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.model.TSData;
import org.opennms.horizon.server.model.TSResult;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLTSDBMetricServiceTest {
    private static final long NODE_ID_1 = 1L;
    private static final long NODE_ID_2 = 2L;
    private static final long LOCATION_ID = 1L;
    private static final String TEST_LOCATION = "test-location1";
    private static final String NODE_SCAN_SCAN_TYPE = "NODE_SCAN";
    private static final String AZURE_SCAN_SCAN_TYPE = "AZURE_SCAN";
    private static final String TEST_TENANT_ID = "test-tenant-id";
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().port(12345));

    @MockBean
    private InventoryClient mockClient;

    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private GraphQLWebTestClient webClient;
    private String accessToken;

    private NodeDTO nodeDTO1, nodeDTO2;

    @BeforeEach
    public void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
        accessToken = webClient.getAccessToken();

        wireMock.start();
        MonitoringLocationDTO locationDTO1 = MonitoringLocationDTO.newBuilder()
                .setId(LOCATION_ID)
                .setLocation(TEST_LOCATION)
                .build();
        nodeDTO1 = NodeDTO.newBuilder()
                .setId(NODE_ID_1)
                .setScanType(NODE_SCAN_SCAN_TYPE)
                .setMonitoringLocationId(locationDTO1.getId())
                .build();
        nodeDTO2 = NodeDTO.newBuilder()
                .setId(NODE_ID_2)
                .setScanType(AZURE_SCAN_SCAN_TYPE)
                .setMonitoringLocationId(locationDTO1.getId())
                .build();
    }

    @AfterEach
    public void after() {
        wireMock.stop();
    }

    @Test
    void getMetricForNodeScanSnmpNetworkInBytes() throws Exception {
        when(mockHeaderUtil.getAuthHeader(any(ResolutionEnvironment.class))).thenReturn(accessToken);
        when(mockClient.getNodeById(eq(NODE_ID_1), eq((accessToken)))).thenReturn(nodeDTO1);
        when(mockHeaderUtil.extractTenant(any(ResolutionEnvironment.class))).thenReturn(TEST_TENANT_ID);

        wireMock.stubFor(post("/api/v1/query")
                .withHeader("X-Scope-OrgID", new EqualToPattern(TEST_TENANT_ID))
                .willReturn(ResponseDefinitionBuilder.okForJson(
                        buildTsQueryResult(NODE_ID_1, "SNMP", "ifInOctets", "sysUpTime"))));

        String request = "query { "
                + "    metric(name: \"network_in_total_bytes\", labels: {node_id: \"1\", monitor: \"SNMP\"}, timeRange: 1, timeRangeUnit: MINUTE) { "
                + "        status, "
                + "        data { "
                + "            resultType, "
                + "            result { "
                + "                metric "
                + "            } "
                + "        } "
                + "    } "
                + "}";
        webClient.exchangeGraphQLQuery(request).expectJsonResponse();
    }

    @Test
    void getMetricForNodeScanSnmpNetworkOutBytes() throws Exception {
        when(mockHeaderUtil.getAuthHeader(any(ResolutionEnvironment.class))).thenReturn(accessToken);
        when(mockClient.getNodeById(eq(NODE_ID_1), eq((accessToken)))).thenReturn(nodeDTO1);
        when(mockHeaderUtil.extractTenant(any(ResolutionEnvironment.class))).thenReturn(TEST_TENANT_ID);

        wireMock.stubFor(post("/api/v1/query")
                .withHeader("X-Scope-OrgID", new EqualToPattern(TEST_TENANT_ID))
                .willReturn(ResponseDefinitionBuilder.okForJson(
                        buildTsQueryResult(NODE_ID_1, "SNMP", "ifOutOctets", "sysUpTime"))));

        String request = "query { "
                + "    metric(name: \"network_out_total_bytes\", labels: {node_id: \"1\", monitor: \"SNMP\"}, timeRange: 1, timeRangeUnit: MINUTE) { "
                + "        status, "
                + "        data { "
                + "            resultType, "
                + "            result { "
                + "                metric "
                + "            } "
                + "        } "
                + "    } "
                + "}";
        webClient.exchangeGraphQLQuery(request).expectJsonResponse();
    }

    @Test
    void getMetricForAzureScanNetworkInBytes() throws Exception {
        when(mockHeaderUtil.getAuthHeader(any(ResolutionEnvironment.class))).thenReturn(accessToken);
        when(mockClient.getNodeById(eq(NODE_ID_2), eq((accessToken)))).thenReturn(nodeDTO2);
        when(mockHeaderUtil.extractTenant(any(ResolutionEnvironment.class))).thenReturn(TEST_TENANT_ID);

        wireMock.stubFor(post("/api/v1/query")
                .withHeader("X-Scope-OrgID", new EqualToPattern(TEST_TENANT_ID))
                .willReturn(ResponseDefinitionBuilder.okForJson(
                        buildTsQueryResult(NODE_ID_2, "AZURE", "network_in_total_bytes"))));

        String request = "query { "
                + "    metric(name: \"network_in_total_bytes\", labels: {node_id: \"2\"}, timeRange: 1, timeRangeUnit: MINUTE) { "
                + "        status, "
                + "        data { "
                + "            resultType, "
                + "            result { "
                + "                metric "
                + "            } "
                + "        } "
                + "    } "
                + "}";
        webClient.exchangeGraphQLQuery(request).expectJsonResponse();
    }

    @Test
    void getMetricForAzureScanNetworkOutBytes() throws Exception {
        when(mockHeaderUtil.getAuthHeader(any(ResolutionEnvironment.class))).thenReturn(accessToken);
        when(mockClient.getNodeById(eq(NODE_ID_2), eq((accessToken)))).thenReturn(nodeDTO2);
        when(mockHeaderUtil.extractTenant(any(ResolutionEnvironment.class))).thenReturn(TEST_TENANT_ID);

        wireMock.stubFor(post("/api/v1/query")
                .withHeader("X-Scope-OrgID", new EqualToPattern(TEST_TENANT_ID))
                .willReturn(ResponseDefinitionBuilder.okForJson(
                        buildTsQueryResult(NODE_ID_2, "AZURE", "network_out_total_bytes"))));

        String request = "query { "
                + "    metric(name: \"network_out_total_bytes\", labels: {node_id: \"2\"}, timeRange: 1, timeRangeUnit: MINUTE) { "
                + "        status, "
                + "        data { "
                + "            resultType, "
                + "            result { "
                + "                metric "
                + "            } "
                + "        } "
                + "    } "
                + "}";
        webClient.exchangeGraphQLQuery(request).expectJsonResponse();
    }

    private TimeSeriesQueryResult buildTsQueryResult(long nodeId, String monitor, String... metricNames) {
        TimeSeriesQueryResult result = new TimeSeriesQueryResult();
        result.setStatus("success");

        TSData data = new TSData();
        data.setResultType("matrix");

        List<TSResult> results = new ArrayList<>();
        for (String metricName : metricNames) {
            results.add(getResult(nodeId, monitor, metricName));
        }

        data.setResult(results);
        result.setData(data);

        return result;
    }

    private static TSResult getResult(long nodeId, String monitor, String metricName) {
        TSResult tsResult = new TSResult();

        Map<String, String> octetsMetric = new HashMap<>();
        octetsMetric.put("__name__", metricName);
        octetsMetric.put("instance", "127.0.0.1");
        octetsMetric.put("job", "horizon-core");
        octetsMetric.put("location", "Default");
        octetsMetric.put("monitor", monitor);
        octetsMetric.put("node_id", String.valueOf(nodeId));
        octetsMetric.put("pushgateway_instance", "horizon-core-pushgateway");
        octetsMetric.put("system_id", "opennms-minion-bf4775678-56dm6");
        tsResult.setMetric(octetsMetric);

        List<List<Double>> values = new ArrayList<>();
        List<Double> value1 = new ArrayList<>();
        value1.add(1.670589032517E9);
        value1.add(0.875);
        values.add(value1);

        List<Double> value2 = new ArrayList<>();
        value2.add(1.823674823642E9);
        value2.add(0.2345);
        values.add(value2);

        tsResult.setValues(values);
        return tsResult;
    }
}
