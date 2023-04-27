package org.opennms.horizon.server.service;

import io.leangen.graphql.execution.ResolutionEnvironment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLLocationServiceTest {
    private static final String GRAPHQL_PATH = "/graphql";
    @MockBean
    private InventoryClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private final String accessToken = "test-token-12345";
    private MonitoringLocationDTO location1, location2;


    @BeforeEach
    public void setUp() {
        location1 = MonitoringLocationDTO.newBuilder().setLocation("LOC1").setTenantId("tenant1").build();
        location2 = MonitoringLocationDTO.newBuilder().setLocation("LOC2").setId(1L).setTenantId("tenant2").build();
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    void testFindLocation() throws JSONException {
        doReturn(Arrays.asList(location1, location2)).when(mockClient).listLocations(accessToken);
        String request = """
            query {
                findAllLocations {
                    location
                    tenantId
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
            .jsonPath("$.data.findAllLocations").isArray()
            .jsonPath("$.data.findAllLocations[0].location").isEqualTo("LOC1")
            .jsonPath("$.data.findAllLocations[0].tenantId").isEqualTo("tenant1")
            .jsonPath("$.data.findAllLocations[1].location").isEqualTo("LOC2")
            .jsonPath("$.data.findAllLocations[1].tenantId").isEqualTo("tenant2");
        verify(mockClient).listLocations(accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testFindLocationById() throws JSONException {
        doReturn(location1).when(mockClient).getLocationById(1, accessToken);
        String request = """
            query {
                findLocationById(id: 1) {
                    location
                    tenantId
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
            .jsonPath("$.data.findLocationById.location").isEqualTo("LOC1")
            .jsonPath("$.data.findLocationById.tenantId").isEqualTo("tenant1");
        verify(mockClient).getLocationById(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testSearchLocation() throws JSONException {
        doReturn(Arrays.asList(location1, location2)).when(mockClient).searchLocations("LOC", accessToken);
        String request = """
            query {
                searchLocation(searchTerm: "LOC") {
                    location
                    tenantId
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
            .jsonPath("$.data.searchLocation").isArray()
            .jsonPath("$.data.searchLocation[0].location").isEqualTo("LOC1")
            .jsonPath("$.data.searchLocation[0].tenantId").isEqualTo("tenant1")
            .jsonPath("$.data.searchLocation[1].location").isEqualTo("LOC2")
            .jsonPath("$.data.searchLocation[1].tenantId").isEqualTo("tenant2");
        verify(mockClient).searchLocations("LOC", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testCreateLocation() throws JSONException {
        MonitoringLocationDTO locationToCreate = MonitoringLocationDTO.newBuilder().setLocation("LOC1").build();
        doReturn(location1).when(mockClient).createLocation(locationToCreate, accessToken);
        String request = """
            mutation {
                createLocation(location: "LOC1") {
                    location
                    tenantId
                    id
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
            .jsonPath("$.data.createLocation.location").isEqualTo("LOC1")
            .jsonPath("$.data.createLocation.tenantId").isEqualTo("tenant1");
        verify(mockClient).createLocation(locationToCreate, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testUpdateLocation() throws JSONException {
        MonitoringLocationDTO locationToUpdate = MonitoringLocationDTO.newBuilder().setId(1L).setLocation("LOC2").build();
        doReturn(location2).when(mockClient).updateLocation(locationToUpdate, accessToken);
        String request = """
            mutation {
                updateLocation(id: 1, location: "LOC2") {
                    location
                    tenantId
                    id
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
            .jsonPath("$.data.updateLocation.location").isEqualTo("LOC2")
            .jsonPath("$.data.updateLocation.id").isEqualTo("1")
            .jsonPath("$.data.updateLocation.tenantId").isEqualTo("tenant2");
        verify(mockClient).updateLocation(locationToUpdate, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testDeleteLocation() throws JSONException {
        doReturn(true).when(mockClient).deleteLocation(1, accessToken);
        String request = """
            mutation {
                deleteLocation(id: 1)
            }""";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.deleteLocation").isEqualTo(true);
        verify(mockClient).deleteLocation(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
