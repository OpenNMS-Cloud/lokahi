package org.opennms.horizon.server.service;

import io.leangen.graphql.execution.ResolutionEnvironment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.AddressCreateDTO;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.dto.AddressUpdateDTO;
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
class GraphQLAddressServiceTest {
    private static final String GRAPHQL_PATH = "/graphql";
    @MockBean
    private InventoryClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private final String accessToken = "test-token-12345";
    private AddressDTO address1, address2;

    @BeforeEach
    public void setUp() {
        address1 = getAddressDTO(1, "addressLine1_1", "addressLine2_1", "city1", "state1", "postalCode1", "country1");
        address2 = getAddressDTO(2, "addressLine1_2", "addressLine2_2", "city2", "state2", "postalCode2", "country2");
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    void testFindAddress() throws JSONException {
        doReturn(Arrays.asList(address1, address2)).when(mockClient).listAddresses(accessToken);
        String request = """
            query {
                findAllAddresses {
                    addressLine1
                    addressLine2
                    city
                    state
                    postalCode
                    country
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
            .jsonPath("$.data.findAllAddresses").isArray()
            .jsonPath("$.data.findAllAddresses[0].addressLine1").isEqualTo("addressLine1_1")
            .jsonPath("$.data.findAllAddresses[0].addressLine2").isEqualTo("addressLine2_1")
            .jsonPath("$.data.findAllAddresses[0].city").isEqualTo("city1")
            .jsonPath("$.data.findAllAddresses[0].state").isEqualTo("state1")
            .jsonPath("$.data.findAllAddresses[0].postalCode").isEqualTo("postalCode1")
            .jsonPath("$.data.findAllAddresses[0].country").isEqualTo("country1")
            .jsonPath("$.data.findAllAddresses[1].addressLine1").isEqualTo("addressLine1_2")
            .jsonPath("$.data.findAllAddresses[1].addressLine2").isEqualTo("addressLine2_2")
            .jsonPath("$.data.findAllAddresses[1].city").isEqualTo("city2")
            .jsonPath("$.data.findAllAddresses[1].state").isEqualTo("state2")
            .jsonPath("$.data.findAllAddresses[1].postalCode").isEqualTo("postalCode2")
            .jsonPath("$.data.findAllAddresses[1].country").isEqualTo("country2");
        verify(mockClient).listAddresses(accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testFindAddressById() throws JSONException {
        doReturn(address1).when(mockClient).getAddressById(1, accessToken);
        String request = """
            query {
                findAddressById(id: 1) {
                    addressLine1
                    addressLine2
                    city
                    state
                    postalCode
                    country
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
            .jsonPath("$.data.findAddressById.addressLine1").isEqualTo("addressLine1_1")
            .jsonPath("$.data.findAddressById.addressLine2").isEqualTo("addressLine2_1")
            .jsonPath("$.data.findAddressById.city").isEqualTo("city1")
            .jsonPath("$.data.findAddressById.state").isEqualTo("state1")
            .jsonPath("$.data.findAddressById.postalCode").isEqualTo("postalCode1")
            .jsonPath("$.data.findAddressById.country").isEqualTo("country1");
        verify(mockClient).getAddressById(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testCreateAddress() throws JSONException {
        AddressCreateDTO addressToCreate = getAddressToCreate();
        doReturn(address1).when(mockClient).createAddress(addressToCreate, accessToken);
        String request = """
            mutation {
                createAddress(address: {
                    addressLine1: "addressLine1"
                    addressLine2: "addressLine2"
                    city: "city"
                    state: "state"
                    postalCode: "postalCode"
                    country: "country"
                }) {
                    addressLine1
                    addressLine2
                    city
                    state
                    postalCode
                    country
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
            .jsonPath("$.data.createAddress.addressLine1").isEqualTo("addressLine1_1")
            .jsonPath("$.data.createAddress.addressLine2").isEqualTo("addressLine2_1")
            .jsonPath("$.data.createAddress.city").isEqualTo("city1")
            .jsonPath("$.data.createAddress.state").isEqualTo("state1")
            .jsonPath("$.data.createAddress.postalCode").isEqualTo("postalCode1")
            .jsonPath("$.data.createAddress.country").isEqualTo("country1");
        verify(mockClient).createAddress(addressToCreate, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testUpdateAddress() throws JSONException {
        AddressUpdateDTO addressToUpdate = getLocationToUpdate();
        doReturn(address2).when(mockClient).updateAddress(addressToUpdate, accessToken);
        String request = """
            mutation {
                updateAddress(address: {
                    id: 2
                    addressLine1: "addressLine1"
                    addressLine2: "addressLine2"
                    city: "city"
                    state: "state"
                    postalCode: "postalCode"
                    country: "country"
                }) {
                    addressLine1
                    addressLine2
                    city
                    state
                    postalCode
                    country
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
            .jsonPath("$.data.updateAddress.addressLine1").isEqualTo("addressLine1_2")
            .jsonPath("$.data.updateAddress.addressLine2").isEqualTo("addressLine2_2")
            .jsonPath("$.data.updateAddress.city").isEqualTo("city2")
            .jsonPath("$.data.updateAddress.state").isEqualTo("state2")
            .jsonPath("$.data.updateAddress.postalCode").isEqualTo("postalCode2")
            .jsonPath("$.data.updateAddress.country").isEqualTo("country2");
        verify(mockClient).updateAddress(addressToUpdate, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testDeleteAddress() throws JSONException {
        doReturn(true).when(mockClient).deleteAddress(1, accessToken);
        String request = """
            mutation {
                deleteAddress(id: 1)
            }""";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.deleteAddress").isEqualTo(true);
        verify(mockClient).deleteAddress(1, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private static AddressUpdateDTO getLocationToUpdate() {
        return AddressUpdateDTO.newBuilder()
            .setId(2)
            .setAddressLine1("addressLine1")
            .setAddressLine2("addressLine2")
            .setCity("city")
            .setState("state")
            .setPostalCode("postalCode")
            .setCountry("country")
            .build();
    }

    private static AddressCreateDTO getAddressToCreate() {
        return AddressCreateDTO.newBuilder()
            .setAddressLine1("addressLine1")
            .setAddressLine2("addressLine2")
            .setCity("city")
            .setState("state")
            .setPostalCode("postalCode")
            .setCountry("country")
            .build();
    }

    private AddressDTO getAddressDTO(int id, String addressLine1, String addressLine2, String city, String state, String postalCode, String country) {
        return AddressDTO.newBuilder()
            .setId(id)
            .setAddressLine1(addressLine1)
            .setAddressLine2(addressLine2)
            .setCity(city)
            .setState(state)
            .setPostalCode(postalCode)
            .setCountry(country)
            .build();
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
