package org.opennms.horizon.server.service;

import com.google.protobuf.ByteString;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.leangen.graphql.execution.ResolutionEnvironment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLMinionCertificateManagerTest {
    private static final String GRAPHQL_PATH = "/graphql";
    public static final String TENANT_ID = "tenantId";
    public static final Long LOCATION_ID = 444L;
    public static final Long INVALID_LOCATION_ID = 404L;

    @MockBean
    private MinionCertificateManagerClient mockClient;
    @MockBean
    private InventoryClient inventoryClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    private final String accessToken = TENANT_ID;

    @BeforeEach
    public void setUp() {
        doReturn(accessToken).when(mockHeaderUtil).extractTenant(any(ResolutionEnvironment.class));
        doReturn(TENANT_ID).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
        var location = MonitoringLocationDTO.newBuilder().setLocation("test").setId(LOCATION_ID).setTenantId(TENANT_ID).build();
        doReturn(location).when(inventoryClient).getLocationById(LOCATION_ID, accessToken);
        var status = Status.newBuilder()
            .setCode(Code.NOT_FOUND_VALUE)
            .setMessage("Given location doesn't exist.").build();
        var exception = StatusProto.toStatusRuntimeException(status);
        doThrow(exception).when(inventoryClient).getLocationById(INVALID_LOCATION_ID, accessToken);
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    void testGetMinionCert() throws JSONException {
        when(mockClient.getMinionCert(TENANT_ID, LOCATION_ID, accessToken)).thenReturn(
            GetMinionCertificateResponse.newBuilder().setCertificate(ByteString.copyFromUtf8("pkcs12-here")).setPassword("passw0rd").build()
        );
        String request = """
            query {
              getMinionCertificate(locationId: 444){
                certificate
                password
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
            .jsonPath("$.data.getMinionCertificate.password").isEqualTo("passw0rd")
            .jsonPath("$.data.getMinionCertificate.certificate").isEqualTo(Base64.getEncoder().encodeToString("pkcs12-here".getBytes()));
        verify(mockClient).getMinionCert(TENANT_ID, LOCATION_ID, accessToken);
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetMinionCertForInvalidLocation() throws JSONException {
        when(mockClient.getMinionCert(TENANT_ID, INVALID_LOCATION_ID, accessToken)).thenReturn(
            GetMinionCertificateResponse.newBuilder().setCertificate(ByteString.copyFromUtf8("pkcs12-here")).setPassword("passw0rd").build()
        );
        String request = """
            query {
              getMinionCertificate(locationId: 404){
                certificate
                password
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
                    .jsonPath("$.data.getMinionCertificate").isEqualTo(null);

        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetMinionCertError() throws JSONException {
            var status = Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Test exception").build();
            var exception = StatusProto.toStatusRuntimeException(status);
            when(inventoryClient.getLocationById(LOCATION_ID, accessToken)).thenThrow(exception);

        String request = """
            query {
              getMinionCertificate(locationId: 404){
                certificate
                password
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
                    .jsonPath("$.data.getMinionCertificate").isEqualTo(null);

        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testRevokeMinionCertificate() throws JSONException {
        String request = """
            mutation {
              revokeMinionCertificate(locationId: 444)
            }""";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.revokeMinionCertificate").isBoolean();
        verify(mockClient,  times(1)).revokeCertificate(TENANT_ID, LOCATION_ID, accessToken);
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testRevokeMinionCertificateForInvalidLocation() throws JSONException {
        String request = """
            mutation {
              revokeMinionCertificate(locationId: %s)
            }""".formatted(INVALID_LOCATION_ID);
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.revokeMinionCertificate").isEmpty()
            .jsonPath("$.errors").isNotEmpty();
        verifyNoInteractions(mockClient);
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testRevokeMinionCertificateError() throws JSONException {
        var status = Status.newBuilder()
            .setCode(Code.INTERNAL_VALUE)
            .setMessage("Test exception").build();
        var exception = StatusProto.toStatusRuntimeException(status);
        when(inventoryClient.getLocationById(LOCATION_ID, accessToken)).thenThrow(exception);

        String request = """
            mutation {
              revokeMinionCertificate(locationId: %s)
            }""".formatted(INVALID_LOCATION_ID);
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.revokeMinionCertificate").isEmpty()
            .jsonPath("$.errors").isNotEmpty();

        verifyNoInteractions(mockClient);
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
