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
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.requireNonNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
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
    public static final String TENANT_ID = "tenantId";
    public static final Long LOCATION_ID = 444L;
    public static final Long INVALID_LOCATION_ID = 404L;

    @MockBean
    private MinionCertificateManagerClient mockClient;
    @MockBean
    private InventoryClient inventoryClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    private GraphQLWebTestClient webClient;
    private String accessToken;

    @BeforeEach
    public void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
        accessToken = webClient.getAccessToken();

        doReturn(TENANT_ID).when(mockHeaderUtil).extractTenant(any(ResolutionEnvironment.class));
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
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
    void testGetMinionCert() throws JSONException, IOException {
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
        JSONObject resultjson = new JSONObject(new String(requireNonNull(
            webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.getMinionCertificate.password").isEqualTo("passw0rd")
                .returnResult()
                .getResponseBody())));

        // Need to check the cert file inside the zip. Start by taking apart the zip and look for a .p12 file
        String zipString = resultjson.getJSONObject("data")
            .getJSONObject("getMinionCertificate")
            .getString("certificate");

        byte[] zipBytes = Base64.getDecoder().decode(zipString.getBytes(StandardCharsets.UTF_8));
        ZipInputStream zstream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        ZipEntry zentry = zstream.getNextEntry();
        while (zstream.available() != 0 && zentry != null && !zentry.getName().contains(".p12")) {
            zstream.closeEntry();
            zentry = zstream.getNextEntry();
        }
        assertNotNull(zentry);
        assertTrue(zentry.getName().contains(".p12"));

        //Decode the cert from the zip so it can be checked
        byte[] certBytes = new byte[2048]; // More than enough for our test file
        int read = zstream.read(certBytes, 0, 2048);
        String certContent = new String(certBytes, 0, read, StandardCharsets.UTF_8);
        assertEquals("Decoded cert from zip file", "pkcs12-here", certContent);

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
        webClient
            .exchangeGraphQLQuery(request)
            .expectJsonResponse()
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
        webClient
            .exchangeGraphQLQuery(request)
            .expectJsonResponse()
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
        webClient
            .exchangeGraphQLQuery(request)
            .expectCleanResponse()
            .jsonPath("$.data.revokeMinionCertificate").isBoolean();
        verify(mockClient, times(1)).revokeCertificate(TENANT_ID, LOCATION_ID, accessToken);
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testRevokeMinionCertificateForInvalidLocation() throws JSONException {
        String request = """
            mutation {
              revokeMinionCertificate(locationId: %s)
            }""".formatted(INVALID_LOCATION_ID);
        webClient
            .exchangeGraphQLQuery(request)
            .expectJsonResponse()
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
        webClient
            .exchangeGraphQLQuery(request)
            .expectJsonResponse()
            .jsonPath("$.data.revokeMinionCertificate").isEmpty()
            .jsonPath("$.errors").isNotEmpty();

        verifyNoInteractions(mockClient);
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }
}
