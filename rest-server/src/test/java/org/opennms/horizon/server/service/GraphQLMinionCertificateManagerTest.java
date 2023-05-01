package org.opennms.horizon.server.service;

import com.google.protobuf.ByteString;
import io.leangen.graphql.execution.ResolutionEnvironment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLMinionCertificateManagerTest {
    private static final String GRAPHQL_PATH = "/graphql";
    public static final String TENANT_ID = "tenantId";
    public static final String LOCATION = "location";

    @MockBean
    private MinionCertificateManagerClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    private final String accessToken = TENANT_ID;

    @BeforeEach
    public void setUp() {
        doReturn(accessToken).when(mockHeaderUtil).extractTenant(any(ResolutionEnvironment.class));
        doReturn(TENANT_ID).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    void testGetMinionCert() throws JSONException {
        doReturn(GetMinionCertificateResponse.newBuilder().setCertificate(ByteString.copyFromUtf8("test")).setPassword("password").build()).when(mockClient).getMinionCert(TENANT_ID, LOCATION, accessToken);
        String request = """
            query {
              getMinionCertificate(location: "location"){
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
            .jsonPath("$.data.getMinionCertificate.password").isEqualTo("password")
            .jsonPath("$.data.getMinionCertificate.certificate").isNotEmpty();
        verify(mockClient).getMinionCert(TENANT_ID, LOCATION, accessToken);
        verify(mockHeaderUtil, times(1)).extractTenant(any(ResolutionEnvironment.class));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
