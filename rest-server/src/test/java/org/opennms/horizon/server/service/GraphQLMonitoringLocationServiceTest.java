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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.util.Collections;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLMonitoringLocationServiceTest {
    @MockBean
    private InventoryClient mockClient;

    @MockBean
    private MinionCertificateManagerClient mockCertificateClient;

    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private GraphQLWebTestClient webClient;
    private String accessToken;
    private static final Long INVALID_LOCATION_ID = 404L;
    private MonitoringLocationDTO location1, location2;

    @BeforeEach
    public void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
        accessToken = webClient.getAccessToken();

        location1 = getLocationDTO("tenant1", "LOC1", 1L, "address1");
        location2 = getLocationDTO("tenant2", "LOC2", 2L, "address2");
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));

        var status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Given location doesn't exist.")
                .build();
        var exception = StatusProto.toStatusRuntimeException(status);

        doThrow(exception).when(mockClient).deleteLocation(INVALID_LOCATION_ID, accessToken);
        doThrow(exception).when(mockClient).getLocationById(INVALID_LOCATION_ID, accessToken);
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    void testFindLocation() throws JSONException {
        doReturn(Collections.singletonList(location1)).when(mockClient).listLocations(accessToken);
        String request =
                """
            query {
                findAllLocations {
                    location
                }
            }""";
        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.findAllLocations")
                .isArray()
                .jsonPath("$.data.findAllLocations[0].location")
                .isEqualTo("LOC1");
        verify(mockClient).listLocations(accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testFindLocationById() throws JSONException {
        doReturn(location1).when(mockClient).getLocationById(1, accessToken);
        String request =
                """
            query {
                findLocationById(id: 1) {
                    location
                }
            }""";
        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.findLocationById.location")
                .isEqualTo("LOC1");
        verify(mockClient).getLocationById(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetLocationByName() throws JSONException {
        doReturn(location1).when(mockClient).getLocationByName("LOC1", accessToken);
        String request =
                """
            query {
                locationByName(locationName: "LOC1") {
                    location
                }
            }""";
        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.locationByName.location")
                .isEqualTo("LOC1");
        verify(mockClient).getLocationByName("LOC1", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testSearchLocation() throws JSONException {
        doReturn(Collections.singletonList(location1)).when(mockClient).searchLocations("LOC", accessToken);
        String request =
                """
            query {
                searchLocation(searchTerm: "LOC") {
                    location
                }
            }""";
        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.searchLocation")
                .isArray()
                .jsonPath("$.data.searchLocation[0].location")
                .isEqualTo("LOC1");
        verify(mockClient).searchLocations("LOC", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testCreateLocation() throws JSONException {
        MonitoringLocationCreateDTO locationToCreate = getLocationToCreate();
        var locationCreated = getLocationDTO("tenant1", "LOC1", 1L, "address create");
        doReturn(locationCreated).when(mockClient).createLocation(locationToCreate, accessToken);
        String request =
                """
            mutation {
                createLocation(location: {
                    location: "LOC1",
                    latitude: 1.0,
                    longitude: 2.0,
                    address: "address create",
                }) {
                    id
                    location
                    latitude
                    longitude
                    address
                }
            }""";

        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.createLocation.location")
                .isEqualTo("LOC1")
                .jsonPath("$.data.createLocation.address")
                .isEqualTo("address create")
                .jsonPath("$.data.createLocation.latitude")
                .isEqualTo(1.0)
                .jsonPath("$.data.createLocation.longitude")
                .isEqualTo(2.0);
        verify(mockClient).createLocation(locationToCreate, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testUpdateLocation() throws JSONException {
        MonitoringLocationDTO locationToUpdate = getLocationToUpdate();
        doReturn(location2).when(mockClient).updateLocation(locationToUpdate, accessToken);
        String request =
                """
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
                    latitude
                    longitude
                    address
                }
            }""";
        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.updateLocation.location")
                .isEqualTo("LOC2")
                .jsonPath("$.data.updateLocation.id")
                .isEqualTo(2)
                .jsonPath("$.data.updateLocation.address")
                .isEqualTo("address2")
                .jsonPath("$.data.updateLocation.latitude")
                .isEqualTo(1.0)
                .jsonPath("$.data.updateLocation.longitude")
                .isEqualTo(2.0);
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
        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.deleteLocation")
                .isEqualTo(true);
        verify(mockClient).deleteLocation(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockCertificateClient).revokeCertificate(any(), eq(1L), eq(accessToken));
    }

    @Test
    void testDeleteNonExistentLocation() throws JSONException {
        String request = """
            mutation {
                deleteLocation(id: %s)
            }"""
                .formatted(INVALID_LOCATION_ID);
        webClient
                .exchangeGraphQLQuery(request)
                .expectJsonResponse()
                .jsonPath("$.data.deleteLocation")
                .isEmpty()
                .jsonPath("$.errors")
                .isNotEmpty();
        verify(mockClient, times(1)).deleteLocation(INVALID_LOCATION_ID, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verifyNoInteractions(mockCertificateClient);
    }

    @Test
    void testDeleteLocationError() throws JSONException {
        var status = Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Test exception")
                .build();
        var exception = StatusProto.toStatusRuntimeException(status);
        doThrow(exception).when(mockClient).deleteLocation(INVALID_LOCATION_ID, accessToken);

        String request = """
            mutation {
                deleteLocation(id: %s)
            }"""
                .formatted(INVALID_LOCATION_ID);
        webClient
                .exchangeGraphQLQuery(request)
                .expectJsonResponse()
                .jsonPath("$.data.deleteLocation")
                .isEmpty()
                .jsonPath("$.errors")
                .isNotEmpty();
        verify(mockClient, times(1)).deleteLocation(INVALID_LOCATION_ID, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verifyNoInteractions(mockCertificateClient);
    }

    private static MonitoringLocationDTO getLocationToUpdate() {
        return MonitoringLocationDTO.newBuilder()
                .setId(1)
                .setLocation("LOC2")
                .setAddress("address2")
                .setGeoLocation(getGeoLocationToCreate())
                .build();
    }

    private static MonitoringLocationCreateDTO getLocationToCreate() {
        return MonitoringLocationCreateDTO.newBuilder()
                .setLocation("LOC1")
                .setAddress("address create")
                .setGeoLocation(getGeoLocationToCreate())
                .build();
    }

    private static GeoLocation getGeoLocationToCreate() {
        return GeoLocation.newBuilder().setLatitude(1.0).setLongitude(2.0).build();
    }

    private MonitoringLocationDTO getLocationDTO(String tenantId, String location, long id, String address) {
        return MonitoringLocationDTO.newBuilder()
                .setId(id)
                .setLocation(location)
                .setTenantId(tenantId)
                .setAddress(address)
                .setGeoLocation(getGeoLocationToCreate())
                .build();
    }
}
