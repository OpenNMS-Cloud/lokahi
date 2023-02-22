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

package org.opennms.horizon.inventory.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialServiceGrpc;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.opennms.horizon.shared.azure.http.dto.error.AzureErrorDescription;
import org.opennms.horizon.shared.azure.http.dto.error.AzureHttpError;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.subscription.AzureSubscription;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.OAUTH2_TOKEN_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.SUBSCRIPTION_ENDPOINT;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class AzureCredentialGrpcItTest extends GrpcTestBase {
    private static final String TEST_NAME = "name";
    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_SUBSCRIPTION_ID = "subscription-id";
    private static final String TEST_DIRECTORY_ID = "directory-id";
    private static final String DEFAULT_LOCATION = "Default";
    private static final String TEST_TAG_NAME_1 = "tag-name-1";

    private AzureCredentialServiceGrpc.AzureCredentialServiceBlockingStub serviceStub;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    @Autowired
    private MonitoringLocationRepository monitoringLocationRepository;

    @Autowired
    private AzureHttpParams params;

    @Autowired
    private TagRepository tagRepository;

    //marking as a @Rule doesn't work, need to manually start/stop in before/after
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().port(12345));

    private static TestTaskSetGrpcService testGrpcService;

    private final ObjectMapper snakeCaseMapper;

    public AzureCredentialGrpcItTest() {
        this.snakeCaseMapper = new ObjectMapper();
        this.snakeCaseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @BeforeAll
    public static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        testGrpcService.reset();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = AzureCredentialServiceGrpc.newBlockingStub(channel);
        wireMock.start();
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        wireMock.stop();
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            tagRepository.deleteAll();
            monitoringLocationRepository.deleteAll();
            azureCredentialRepository.deleteAll();
        });
        testGrpcService.reset();
        afterTest();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @Test
    void testCreateAzureCredentials() throws Exception {
        mockAzureLogin();
        mockAzureGetSubscription(true);

        TagCreateDTO tagCreateDto1 = TagCreateDTO.newBuilder().setName(TEST_TAG_NAME_1).build();

        AzureCredentialCreateDTO createDTO = AzureCredentialCreateDTO.newBuilder()
            .setLocation(DEFAULT_LOCATION)
            .setName(TEST_NAME)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .addAllTags(List.of(tagCreateDto1))
            .build();

        AzureCredentialDTO credentials = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createCredentials(createDTO);

        assertTrue(credentials.getId() > 0);

        await().atMost(10, TimeUnit.SECONDS).until( () ->  testGrpcService.getTaskDefinitions(DEFAULT_LOCATION).stream()
            .filter(taskDef -> taskDef.getType().equals(TaskType.SCANNER)).toList().size(), Matchers.is(1));

        assertEquals(createDTO.getClientId(), credentials.getClientId());
        assertEquals(createDTO.getSubscriptionId(), credentials.getSubscriptionId());
        assertEquals(createDTO.getDirectoryId(), credentials.getDirectoryId());
        assertTrue(credentials.getCreateTimeMsec() > 0L);

        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            List<AzureCredential> list = azureCredentialRepository.findAll();
            assertEquals(1, list.size());

            AzureCredential azureCredential = list.get(0);
            assertTrue(azureCredential.getId() > 0);
            assertNotNull(azureCredential.getMonitoringLocation());
            assertEquals(createDTO.getName(), azureCredential.getName());
            assertEquals(createDTO.getLocation(), azureCredential.getMonitoringLocation().getLocation());
            assertEquals(createDTO.getClientId(), azureCredential.getClientId());
            assertEquals(createDTO.getClientSecret(), azureCredential.getClientSecret());
            assertEquals(createDTO.getSubscriptionId(), azureCredential.getSubscriptionId());
            assertEquals(createDTO.getDirectoryId(), azureCredential.getDirectoryId());
            assertNotNull(azureCredential.getCreateTime());

            List<Tag> allTags = tagRepository.findAll();
            assertEquals(1, allTags.size());

            Tag tag = allTags.get(0);
            assertEquals(tagCreateDto1.getName(), tag.getName());
            assertEquals(1, tag.getAzureCredentials().size());

            AzureCredential credential = tag.getAzureCredentials().get(0);
            assertEquals(azureCredential.getId(), credential.getId());
        });



        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateAzureCredentialsFailedSubscription() throws Exception {
        mockAzureLogin();
        mockAzureGetSubscriptionFailed();

        AzureCredentialCreateDTO createDTO = AzureCredentialCreateDTO.newBuilder()
            .setName(TEST_NAME)
            .setLocation(DEFAULT_LOCATION)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createCredentials(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertEquals("Code: Message", status.getMessage());
        assertThat(status.getCode()).isEqualTo(Code.INTERNAL_VALUE);
        assertEquals(0, testGrpcService.getRequests().size());
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateAzureCredentialsDisabledSubscription() throws Exception {
        mockAzureLogin();
        mockAzureGetSubscription(false);

        AzureCredentialCreateDTO createDTO = AzureCredentialCreateDTO.newBuilder()
            .setName(TEST_NAME)
            .setLocation(DEFAULT_LOCATION)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createCredentials(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertThat(status.getCode()).isEqualTo(Code.INTERNAL_VALUE);
        assertEquals(0, testGrpcService.getRequests().size());
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateAzureCredentialsWithoutTenantId() throws VerificationException {

        AzureCredentialCreateDTO createDTO = AzureCredentialCreateDTO.newBuilder()
            .setName(TEST_NAME)
            .setLocation(DEFAULT_LOCATION)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .build();

        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, () ->
            serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant)))
                .createCredentials(createDTO));
        assertThat(exception.getStatus().getCode()).isEqualTo(io.grpc.Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Missing tenant id");
        verify(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    private void mockAzureLogin() throws JsonProcessingException {
        String url = String.format(OAUTH2_TOKEN_ENDPOINT, TEST_DIRECTORY_ID)
            + "?api-version=" + this.params.getApiVersion();

        wireMock.stubFor(post(url)
            .withHeader("Content-Type", new EqualToPattern("application/x-www-form-urlencoded"))
            .willReturn(ResponseDefinitionBuilder.responseDefinition()
                .withStatus(HttpStatus.SC_OK)
                .withBody(snakeCaseMapper.writeValueAsString(getAzureOAuthToken()))));
    }

    private void mockAzureGetSubscription(boolean enabled) {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(SUBSCRIPTION_ENDPOINT, TEST_SUBSCRIPTION_ID)
            + "?api-version=" + this.params.getApiVersion();

        AzureSubscription azureSubscription = new AzureSubscription();
        azureSubscription.setSubscriptionId(TEST_SUBSCRIPTION_ID);
        azureSubscription.setState((enabled) ? "Enabled" : "Disabled");

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
            .willReturn(ResponseDefinitionBuilder.okForJson(azureSubscription)));
    }

    private void mockAzureGetSubscriptionFailed() {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(SUBSCRIPTION_ENDPOINT, TEST_SUBSCRIPTION_ID)
            + "?api-version=" + this.params.getApiVersion();

        AzureHttpError error = new AzureHttpError();
        AzureErrorDescription description = new AzureErrorDescription();
        description.setCode("Code");
        description.setMessage("Message");
        error.setError(description);

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
            .willReturn(ResponseDefinitionBuilder.responseDefinition()
                .withStatus(500).withHeader("Content-Type", "application/json")
                .withBody(Json.write(error))));
    }

    private AzureOAuthToken getAzureOAuthToken() {
        AzureOAuthToken token = new AzureOAuthToken();
        token.setTokenType("Bearer");
        token.setExpiresIn("3599");
        token.setExtExpiresIn("3599");
        token.setExpiresOn("1673347297");
        token.setNotBefore("1673347297");
        token.setResource(wireMock.baseUrl());
        token.setAccessToken("access-token");
        return token;
    }

}
