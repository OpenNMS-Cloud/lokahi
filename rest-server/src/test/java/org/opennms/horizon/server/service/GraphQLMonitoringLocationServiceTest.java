package org.opennms.horizon.server.service;

import io.leangen.graphql.execution.ResolutionEnvironment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
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
class GraphQLMonitoringLocationServiceTest {
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
        location1 = getLocationDTO("tenant1", "LOC1", 1L, "address1");
        location2 = getLocationDTO("tenant2", "LOC2", 2L, "address2");
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
        MonitoringLocationCreateDTO locationToCreate = getLocationToCreate();
        var locationCreated = getLocationDTO("tenant1", "LOC1", 1L, "address create");
        doReturn(locationCreated).when(mockClient).createLocation(locationToCreate, accessToken);
        String request = """
            mutation {
                createLocation(location: {
                    location: "LOC1",
                    latitude: 1.0,
                    longitude: 2.0,
                    address: "address create",
                }) {
                    id
                    location
                    tenantId
                    latitude
                    longitude
                    address
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
            .jsonPath("$.data.createLocation.tenantId").isEqualTo("tenant1")
            .jsonPath("$.data.createLocation.address").isEqualTo("address create")
            .jsonPath("$.data.createLocation.latitude").isEqualTo(1.0)
            .jsonPath("$.data.createLocation.longitude").isEqualTo(2.0);
        verify(mockClient).createLocation(locationToCreate, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testUpdateLocation() throws JSONException {
        MonitoringLocationDTO locationToUpdate = getLocationToUpdate();
        doReturn(location2).when(mockClient).updateLocation(locationToUpdate, accessToken);
        String request = """
            mutation {
                updateLocation(location: {
                    id: 1,
                    location: "LOC2",
                    latitude: 1.0,
                    longitude: 2.0,
                    address: "address2"
                }) {
                    id
                    location
                    tenantId
                    latitude
                    longitude
                    address
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
            .jsonPath("$.data.updateLocation.id").isEqualTo(2)
            .jsonPath("$.data.updateLocation.tenantId").isEqualTo("tenant2")
            .jsonPath("$.data.updateLocation.address").isEqualTo("address2")
            .jsonPath("$.data.updateLocation.latitude").isEqualTo(1.0)
            .jsonPath("$.data.updateLocation.longitude").isEqualTo(2.0);
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

    private static MonitoringLocationDTO getLocationToUpdate() {
        return MonitoringLocationDTO.newBuilder()
            .setId(1)
            .setLocation("LOC2")
            .setAddress("address2")
            .setGeoLocation(getGeoLocationToCreate()).build();
    }

    private static MonitoringLocationCreateDTO getLocationToCreate() {
        return MonitoringLocationCreateDTO.newBuilder()
            .setLocation("LOC1")
            .setAddress("address create")
            .setGeoLocation(getGeoLocationToCreate())
            .build();
    }

    private static GeoLocation getGeoLocationToCreate() {
        return GeoLocation.newBuilder()
            .setLatitude(1.0)
            .setLongitude(2.0)
            .build();
    }

    private MonitoringLocationDTO getLocationDTO(String tenantId, String location, long id, String address) {
        return MonitoringLocationDTO.newBuilder()
            .setId(id)
            .setLocation(location)
            .setTenantId(tenantId)
            .setAddress(address)
            .setGeoLocation(getGeoLocationToCreate()).build();
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
