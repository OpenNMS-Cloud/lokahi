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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.server.model.TSData;
import org.opennms.horizon.server.model.TSResult;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.metrics.TSDBMetricsService;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.leangen.graphql.execution.ResolutionEnvironment;
import reactor.core.publisher.Mono;

//This purpose of this test class is keep checking the dataloader logic is correct.
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
public class GraphQLNodeServiceTest {
    private static final String GRAPHQL_PATH="/graphql";
    @MockBean
    private InventoryClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    @MockBean
    private TSDBMetricsService tsdbMetricsService;
    private final String accessToken = "test-token-12345";
    private MonitoringLocationDTO locationDTO1, locationDTO2;
    private NodeDTO nodeDTO1, nodeDTO2, nodeDTO3, nodeDTO4;
    @Captor
    private ArgumentCaptor<List<DataLoaderFactory.Key>> keyCaptor;

    @BeforeEach
    public void setUp() {
        locationDTO1 = MonitoringLocationDTO.newBuilder().setId(1L).setLocation("test-location1").build();
        locationDTO2 = MonitoringLocationDTO.newBuilder().setId(2L).setLocation("test-location2").build();
        nodeDTO1 = NodeDTO.newBuilder().setId(1L).setMonitoringLocationId(locationDTO1.getId()).build();
        nodeDTO2 = NodeDTO.newBuilder().setId(2L).setMonitoringLocationId(locationDTO1.getId()).build();
        nodeDTO3 = NodeDTO.newBuilder().setId(3L).setMonitoringLocationId(locationDTO2.getId()).build();

        IpInterfaceDTO ipInterfaceDTO = IpInterfaceDTO.newBuilder().setIpAddress("127.0.0.1").build();
        nodeDTO4 = NodeDTO.newBuilder().setId(4L).setMonitoringLocationId(locationDTO2.getId()).addIpInterfaces(ipInterfaceDTO).build();

        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @AfterEach
    public void afterTest(){
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    public void testListNodes() throws JSONException {
        doReturn(Arrays.asList(nodeDTO1, nodeDTO2, nodeDTO3)).when(mockClient).listNodes(accessToken);
        doReturn(Arrays.asList(locationDTO1, locationDTO2)).when(mockClient).listLocationsByIds(keyCaptor.capture());
        String request = "query {findAllNodes {id location {location}}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findAllNodes.size()").isEqualTo(3)
            .jsonPath("$.data.findAllNodes[0].location.location").isEqualTo(locationDTO1.getLocation())
            .jsonPath("$.data.findAllNodes[1].location.location").isEqualTo(locationDTO1.getLocation())
            .jsonPath("$.data.findAllNodes[2].location.location").isEqualTo(locationDTO2.getLocation());
        verify(mockClient).listNodes(accessToken);
        verify(mockHeaderUtil, times(4)).getAuthHeader(any(ResolutionEnvironment.class));
        verify(mockClient).listLocationsByIds(keyCaptor.capture());
        List<DataLoaderFactory.Key> argus = keyCaptor.getValue();
        assertThat(argus.size()).isEqualTo(2);
    }

    @Test
    public void testFindAllNodesByNodeLabelSearch() throws JSONException {
        doReturn(Arrays.asList(nodeDTO1, nodeDTO2, nodeDTO3)).when(mockClient)
            .listNodesByNodeLabelSearch("test-search-term", accessToken);
        String request = "query {\n" +
            "    findAllNodesByNodeLabelSearch(labelSearchTerm: \"test-search-term\") {\n" +
            "       id, " +
            "       nodeLabel, " +
            "       createTime " +
            "    } " +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findAllNodesByNodeLabelSearch.size()").isEqualTo(3);
        verify(mockClient).listNodesByNodeLabelSearch("test-search-term", accessToken);
        verify(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testFindAllNodesByTags() throws JSONException {
        List<String> tags = Arrays.asList("tag1");
        doReturn(Arrays.asList(nodeDTO1, nodeDTO2, nodeDTO3)).when(mockClient)
            .listNodesByTags(tags, accessToken);
        String request = "query { " +
            "    findAllNodesByTags(tags: [\"tag1\"]) { " +
            "       id, " +
            "       nodeLabel, " +
            "       createTime " +
            "    } " +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findAllNodesByTags.size()").isEqualTo(3);
        verify(mockClient).listNodesByTags(tags, accessToken);
        verify(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testListNodesSkipLocation() throws JSONException {
        doReturn(Arrays.asList(nodeDTO1, nodeDTO2, nodeDTO3)).when(mockClient).listNodes(accessToken);
        String request = "query {findAllNodes {id}}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findAllNodes.size()").isEqualTo(3)
            .jsonPath("$.data.findAllNodes[0].location").doesNotExist()
            .jsonPath("$.data.findAllNodes[1].location").doesNotExist()
            .jsonPath("$.data.findAllNodes[2].location").doesNotExist();
        verify(mockClient).listNodes(accessToken);
        verify(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testFindNodeById() throws JSONException {
        doReturn(nodeDTO1).when(mockClient).getNodeById(nodeDTO1.getId(), accessToken);
        doReturn(Collections.singletonList(locationDTO1)).when(mockClient).listLocationsByIds(keyCaptor.capture());
        String request = createPayload("query{findNodeById(id: " + nodeDTO1.getId() +
            ") {id location {location} nodeLabel}}");
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findNodeById.id").isEqualTo(nodeDTO1.getId())
            .jsonPath("$.data.findNodeById.location.location").isEqualTo(locationDTO1.getLocation());
        verify(mockClient).getNodeById(nodeDTO1.getId(), accessToken);
        verify(mockHeaderUtil, times(2)).getAuthHeader(any(ResolutionEnvironment.class));
        verify(mockClient).listLocationsByIds(keyCaptor.capture());
        List<DataLoaderFactory.Key> keys = keyCaptor.getValue();
        assertThat(keys.size()).isEqualTo(1);
    }

    @Test
    public void testFindNodeByIdSkipLocation() throws JSONException {
        doReturn(nodeDTO1).when(mockClient).getNodeById(nodeDTO1.getId(), accessToken);
        String request = createPayload("query{findNodeById(id: " + nodeDTO1.getId() +
            ") {id nodeLabel}}");
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.findNodeById.id").isEqualTo(nodeDTO1.getId())
            .jsonPath("$.data.findNodeById.location").doesNotExist();
        verify(mockClient).getNodeById(nodeDTO1.getId(), accessToken);
        verify(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testCreateNode() throws JSONException {
        doReturn(nodeDTO1).when(mockClient).createNewNode(any(NodeCreateDTO.class), eq(accessToken));
        doReturn(Collections.singletonList(locationDTO1)).when(mockClient).listLocationsByIds(keyCaptor.capture());
        String request = createPayload("mutation {addNode(node: {label: \"test-node\", locationId: \"1000\", managementIp: \"127.0.0.1\", tags: [{name:\"tag-10\"}]})" +
            "{id nodeLabel}}");
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.addNode.id").isEqualTo(nodeDTO1.getId())
            .jsonPath("$.data.addNode.nodeLabel").isEqualTo(nodeDTO1.getNodeLabel());
        verify(mockClient).createNewNode(any(NodeCreateDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testCreateNodeSkipLocationInReturn() throws JSONException {
        doReturn(nodeDTO1).when(mockClient).createNewNode(any(NodeCreateDTO.class), eq(accessToken));
        String request = createPayload("mutation {addNode(node: {label: \"test-node\" managementIp: \"127.0.0.1\"})" +
            "{id nodeLabel}}");
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.addNode.id").isEqualTo(nodeDTO1.getId())
            .jsonPath("$.data.addNode.location").doesNotExist()
            .jsonPath("$.data.addNode.nodeLabel").exists();
        verify(mockClient).createNewNode(any(NodeCreateDTO.class), eq(accessToken));
        verify(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testGetNodeStatusUp() throws JSONException {
        TimeSeriesQueryResult tsQueryResult = buildTsQueryResult(true);

        doReturn(nodeDTO4).when(mockClient).getNodeById(anyLong(), eq(accessToken));
        doReturn(Mono.just(tsQueryResult)).when(tsdbMetricsService)
            .getMetric(any(ResolutionEnvironment.class), anyString(), anyMap(), anyInt(), any(TimeRangeUnit.class));

        String query = String.format("query { nodeStatus(id: %d) { id, status }}", nodeDTO4.getId());
        String request = createPayload(query);
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.nodeStatus.id").isEqualTo(nodeDTO4.getId())
            .jsonPath("$.data.nodeStatus.status").isEqualTo("UP");
        verify(mockClient).getNodeById(eq(nodeDTO4.getId()), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testGetNodeStatusDown() throws JSONException {
        TimeSeriesQueryResult tsQueryResult = buildTsQueryResult(false);

        doReturn(nodeDTO4).when(mockClient).getNodeById(anyLong(), eq(accessToken));
        doReturn(Mono.just(tsQueryResult)).when(tsdbMetricsService)
            .getMetric(any(ResolutionEnvironment.class), anyString(), anyMap(), anyInt(), any(TimeRangeUnit.class));

        String query = String.format("query { nodeStatus(id: %d) { id, status }}", nodeDTO4.getId());
        String request = createPayload(query);
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.nodeStatus.id").isEqualTo(nodeDTO4.getId())
            .jsonPath("$.data.nodeStatus.status").isEqualTo("DOWN");
        verify(mockClient).getNodeById(eq(nodeDTO4.getId()), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    public void testGetNodeStatusNoIpInterface() throws JSONException {
        doReturn(nodeDTO3).when(mockClient).getNodeById(anyLong(), eq(accessToken));

        String query = String.format("query { nodeStatus(id: %d) { id, status }}", nodeDTO3.getId());
        String request = createPayload(query);
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.nodeStatus.id").isEqualTo(nodeDTO3.getId())
            .jsonPath("$.data.nodeStatus.status").isEqualTo("DOWN");
        verify(mockClient).getNodeById(eq(nodeDTO3.getId()), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }

    private TimeSeriesQueryResult buildTsQueryResult(boolean successful) {
        TimeSeriesQueryResult result = new TimeSeriesQueryResult();
        result.setStatus("success");

        TSData data = new TSData();
        data.setResultType("matrix");

        TSResult tsResult = new TSResult();

        Map<String, String> metric = new HashMap<>();
        metric.put("__name__", "response_time_msec");
        metric.put("instance", "127.0.0.1");
        metric.put("job", "horizon-core");
        metric.put("location", "Default");
        metric.put("monitor", "ICMP");
        metric.put("node_id", "1");
        metric.put("pushgateway_instance", "horizon-core-pushgateway");
        metric.put("system_id", "opennms-minion-bf4775678-56dm6");
        tsResult.setMetric(metric);

        if (successful) {
            List<List<Double>> values = new ArrayList<>();
            List<Double> value = new ArrayList<>();
            value.add(1.670589032517E9);
            value.add(0.875);
            values.add(value);
            tsResult.setValues(values);
        }

        data.setResult(Collections.singletonList(tsResult));
        result.setData(data);

        return result;
    }
}
