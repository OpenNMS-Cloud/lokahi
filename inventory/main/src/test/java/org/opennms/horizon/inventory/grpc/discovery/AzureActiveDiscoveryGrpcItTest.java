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

package org.opennms.horizon.inventory.grpc.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.grpc.GrpcTestBase;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.inventory.repository.discovery.active.AzureActiveDiscoveryRepository;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.opennms.horizon.shared.azure.http.dto.error.AzureErrorDescription;
import org.opennms.horizon.shared.azure.http.dto.error.AzureHttpError;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.subscription.AzureSubscription;
import org.opennms.taskset.contract.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

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
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.OAUTH2_TOKEN_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.SUBSCRIPTION_ENDPOINT;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@AutoConfigureObservability     // Make sure to include Metrics (for some reason they are disabled by default in the integration grey-box test)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled
class AzureActiveDiscoveryGrpcItTest extends GrpcTestBase {
    private static final String TEST_NAME = "name";
    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_SUBSCRIPTION_ID = "subscription-id";
    private static final String TEST_DIRECTORY_ID = "directory-id";
    private static final String DEFAULT_LOCATION = "Default";
    private static final String TEST_TAG_NAME_1 = "tag-name-1";
    private static final String TEST_TENANT_ID = "tenant-id";

    private AzureActiveDiscoveryServiceGrpc.AzureActiveDiscoveryServiceBlockingStub serviceStub;

    @Autowired
    private AzureActiveDiscoveryRepository repository;

    @Autowired
    private AzureHttpParams params;

    @Autowired
    private TagRepository tagRepository;

    //marking as a @Rule doesn't work, need to manually start/stop in before/after
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().port(12345));

    private final ObjectMapper snakeCaseMapper;

    public AzureActiveDiscoveryGrpcItTest() {
        this.snakeCaseMapper = new ObjectMapper();
        this.snakeCaseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareTestGrpc();
        prepareServer();
        serviceStub = AzureActiveDiscoveryServiceGrpc.newBlockingStub(channel);
        wireMock.start();
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        wireMock.stop();
        afterTest();
    }


    @Test
    void testCreateAzureActiveDiscovery() throws Exception {
        mockAzureLogin();
        mockAzureGetSubscription(true);

        TagCreateDTO tagCreateDto1 = TagCreateDTO.newBuilder().setName(TEST_TAG_NAME_1).build();

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
            .setLocation(DEFAULT_LOCATION)
            .setName(TEST_NAME)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .addAllTags(List.of(tagCreateDto1))
            .build();

        AzureActiveDiscoveryDTO dicsovery = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createDiscovery(createDTO);

        assertTrue(dicsovery.getId() > 0);

        await().atMost(10, TimeUnit.SECONDS).until(() -> testGrpcService.getTaskDefinitions(DEFAULT_LOCATION).stream()
            .filter(taskDef -> taskDef.getType().equals(TaskType.SCANNER)).toList().size(), Matchers.is(1));

        assertEquals(createDTO.getClientId(), dicsovery.getClientId());
        assertEquals(createDTO.getSubscriptionId(), dicsovery.getSubscriptionId());
        assertEquals(createDTO.getDirectoryId(), dicsovery.getDirectoryId());
        assertTrue(dicsovery.getCreateTimeMsec() > 0L);

        List<AzureActiveDiscovery> list = repository.findAll();
        assertEquals(1, list.size());

        AzureActiveDiscovery discovery = list.get(0);
        assertTrue(discovery.getId() > 0);
        assertNotNull(discovery.getLocation());
        assertEquals(createDTO.getName(), discovery.getName());
        assertEquals(createDTO.getLocation(), discovery.getLocation());
        assertEquals(createDTO.getClientId(), discovery.getClientId());
        assertEquals(createDTO.getClientSecret(), discovery.getClientSecret());
        assertEquals(createDTO.getSubscriptionId(), discovery.getSubscriptionId());
        assertEquals(createDTO.getDirectoryId(), discovery.getDirectoryId());
        assertNotNull(discovery.getCreateTime());

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(1, allTags.size());

        Tag tag = allTags.get(0);
        assertEquals(tagCreateDto1.getName(), tag.getName());
        assertEquals(1, tag.getActiveDiscoveries().size());

        ActiveDiscovery activeDiscovery = tag.getActiveDiscoveries().get(0);
        assertEquals(discovery.getId(), activeDiscovery.getId());
    }

    @Test
    void testCreateAzureActiveDiscoveryFailedSubscription() throws Exception {
        mockAzureLogin();
        mockAzureGetSubscriptionFailed();

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
            .setName(TEST_NAME)
            .setLocation(DEFAULT_LOCATION)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createDiscovery(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertEquals("Code: Message", status.getMessage());
        assertThat(status.getCode()).isEqualTo(Code.INTERNAL_VALUE);
        assertEquals(0, testGrpcService.getRequests().size());
    }

    @Test
    void testCreateAzureActiveDiscoveryDisabledSubscription() throws Exception {
        mockAzureLogin();
        mockAzureGetSubscription(false);

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
            .setName(TEST_NAME)
            .setLocation(DEFAULT_LOCATION)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createDiscovery(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertThat(status.getCode()).isEqualTo(Code.INTERNAL_VALUE);
        assertEquals(0, testGrpcService.getRequests().size());
    }

    @Test
    void testCreateAzureActiveDiscoveryAlreadyExists() throws Exception {
        mockAzureLogin();
        mockAzureGetSubscription(true);

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
            .setName(TEST_NAME)
            .setLocation(DEFAULT_LOCATION)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .build();

        serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createDiscovery(createDTO);

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createDiscovery(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertThat(status.getCode()).isEqualTo(Code.INTERNAL_VALUE);
        assertThat(exception.getMessage()).contains("Azure Discovery already exists with the provided subscription, directory and client ID");
    }

    @Test
    void testCreateAzureActiveDicscoveryWithoutTenantId() {

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
            .setName(TEST_NAME)
            .setLocation(DEFAULT_LOCATION)
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () ->
            serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant)))
                .createDiscovery(createDTO));
        assertThat(exception.getStatus().getCode()).isEqualTo(io.grpc.Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Missing tenant id");
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
