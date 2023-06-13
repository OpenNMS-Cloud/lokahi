/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.systemtests.api.portal;

import okhttp3.ResponseBody;
import org.junit.Assert;
import org.opennms.horizon.systemtests.api.ServiceGenerator;
import org.opennms.horizon.systemtests.api.portal.models.AuthnRequest;
import org.opennms.horizon.systemtests.api.portal.models.AuthnResponse;
import org.opennms.horizon.systemtests.api.portal.models.CloudInstanceRequest;
import org.opennms.horizon.systemtests.api.portal.models.CloudInstancesResponse;
import org.opennms.horizon.systemtests.api.portal.models.GetInstanceUsersResponse;
import org.opennms.horizon.systemtests.api.portal.models.AddInstanceUserRequest;
import org.opennms.horizon.systemtests.api.portal.models.TokenResponse;
import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;


public class PortalApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortalApi.class);
    private final OktaEndpoints oktaService;
    private final PortalEndpoints portalService;
    private final String authToken;

    public PortalApi() {
        oktaService = ServiceGenerator.createService(SecretsStorage.oktaHost, OktaEndpoints.class);
        portalService = ServiceGenerator.createService(SecretsStorage.portalHost, PortalEndpoints.class);

        String sessionToken = authn();
        String code = authorize(sessionToken);
        authToken = getAuthToken(code);
    }

    /**
     * @param instanceName - a name of BTO instance
     * @param adminEmail   - an email of assigned admin
     * @return the string with an instance uuid
     */
    public String createCloudInstance(String instanceName, String adminEmail) {
        try {
            Response<ResponseBody> btoInstanceId = portalService.createCloudInstance(
                SecretsStorage.portalOrganizationId,
                new CloudInstanceRequest(instanceName, adminEmail),
                authToken
            ).execute();

            if (btoInstanceId.isSuccessful()) {
                return btoInstanceId.body().string();
            }
            Assert.fail("[TEST] Creating a cloud instance failed: " + btoInstanceId.errorBody().string());
        } catch (IOException ex) {
            Assert.fail("[TEST] Failed creating a cloud instance: " + ex.getMessage());
        }
        return null;
    }

    public CloudInstancesResponse listCloudInstances() {
        try {
            Response<CloudInstancesResponse> instances = portalService.getCloudInstances(
                SecretsStorage.portalOrganizationId,
                null,
                null,
                100,
                authToken
            ).execute();

            if (!instances.isSuccessful()) {
                Assert.fail("[TEST] listing cloud instances failed with error " + instances.errorBody().string());
            }
            return instances.body();
        } catch (IOException e) {
            Assert.fail("[TEST] failed listing cloud instances: " + e.getLocalizedMessage());
        }
        return null;
    }

    public CloudInstancesResponse searchCloudInstancesByName(String searchPattern) {
        try {
            Response<CloudInstancesResponse> instances = portalService.getCloudInstances(
                SecretsStorage.portalOrganizationId,
                searchPattern,
                "name",
                100,
                authToken
            ).execute();

            if (!instances.isSuccessful()) {
                Assert.fail("[TEST] listing cloud instances by name failed with error: " + instances.errorBody().string());
            }
            return instances.body();
        } catch (IOException e) {
            Assert.fail("[TEST] failed listing cloud instances by name: " + e.getLocalizedMessage());
        }
        return null;
    }

    public void deleteCloudInstance(String cloudInstanceId) {
        try {
            Response<Void> deleteResponse = portalService.deleteInstance(
                SecretsStorage.portalOrganizationId,
                cloudInstanceId,
                authToken
            ).execute();

            if (!deleteResponse.isSuccessful()) {
                Assert.fail("[TEST] deleting cloud instance failed with error " + deleteResponse.errorBody().string());
            }
        } catch (IOException e) {
            Assert.fail("[TEST] failed deleting cloud instance: " + e.getLocalizedMessage());
        }
    }

    public GetInstanceUsersResponse getAllInstancesUsers(String instanceId) {
        try {
            Response<GetInstanceUsersResponse> response = portalService.getInstanceUsers(
                SecretsStorage.portalOrganizationId,
                instanceId,
                10,
                0,
                authToken
            ).execute();

            if (!response.isSuccessful()) {
                Assert.fail("[TEST] listing users associated to cloud instance failed with error:" + response.errorBody().string());
            }

            return response.body();
        } catch (IOException e) {
            Assert.fail("[TEST] failed listing users associated to cloud instance: " + e.getLocalizedMessage());
        }
        return null;
    }

    public void addUserToInstance(String instanceName, String userEmail) {
        String instanceId = searchCloudInstancesByName(instanceName).pagedRecords.get(0).id;

        try {
            Response<Void> response = portalService.addInstanceUser(
                SecretsStorage.portalOrganizationId,
                instanceId,
                new AddInstanceUserRequest(userEmail),
                authToken
            ).execute();

            if (!response.isSuccessful()) {
                Assert.fail("[TEST] adding user to cloud instance failed with error:" + response.errorBody().string());
            }
        } catch (IOException e) {
            Assert.fail("[TEST] failed adding user to cloud instance: " + e.getLocalizedMessage());
        }
    }

    public void deleteUserFromInstance(String instanceId, String userIdentity) {
        try {
            Response<Void> response = portalService.deleteInstanceUser(
                SecretsStorage.portalOrganizationId,
                instanceId,
                userIdentity,
                authToken
            ).execute();

            if (!response.isSuccessful()) {
                Assert.fail("[TEST] deleting user from cloud instance failed with error:" + response.errorBody().string());
            }
        } catch (IOException e) {
            Assert.fail("[TEST] failed deleting user from cloud instance: " + e.getLocalizedMessage());
        }
    }

    public void revokeUserAccessFromInstance(String instanceName, String userEmail) {
        String instanceId = searchCloudInstancesByName(instanceName).pagedRecords.get(0).id;
        GetInstanceUsersResponse.InstanceUser user = getAllInstancesUsers(instanceId)
            .pagedRecords
            .stream().filter(t -> t.email.equals(userEmail))
            .findFirst().get();
        deleteUserFromInstance(instanceId, user.identity);
    }

    public void deleteAllCloudInstances() {
        List<CloudInstancesResponse.Instance> items = listCloudInstances().pagedRecords;

        for (CloudInstancesResponse.Instance item : items) {
            deleteCloudInstance(item.id);
            LOGGER.info("[TEST] cloud instance {} deleted", item.name);
        }
    }

    private String authn() {
        Response<AuthnResponse> authn = null;
        try {
            authn = oktaService.authn(new AuthnRequest()).execute();
            if (!authn.isSuccessful()) {
                Assert.fail(authn.errorBody().string());
            }
        } catch (IOException e) {
            Assert.fail("[TEST] Request to Portal /authn failed: " + e.getMessage());
        }
        Assert.assertNotNull(authn);
        LOGGER.info("[TEST] Okta session_token: {}", authn.body().sessionToken);
        return authn.body().sessionToken;
    }

    private String authorize(String sessionToken) {
        Response<ResponseBody> authorize;
        try {
            authorize = oktaService.authorize(
                SecretsStorage.oktaCompanyId,
                SecretsStorage.oktaClientId,
                SecretsStorage.oktaCodeChallenge,
                SecretsStorage.oktaCodeChallengeMethod,
                SecretsStorage.oktaNonce,
                "none",
                "openid",
                SecretsStorage.portalHost,
                sessionToken,
                SecretsStorage.oktaState,
                "query",
                "code"
            ).execute();

            // the code is located in the `location` header:
            // location: https://xxx.cloud.opennms.com?code={code}&state={state}

            String code = authorize.headers().get("location").split("code=")[1].split("&")[0];

            LOGGER.info("[TEST] OKTA code: {}", code);
            return code;
        } catch (IOException ex) {
            Assert.fail("[TEST] Cannot parse the response from /authorize: " + ex);
        }
        return null;
    }

    private String getAuthToken(String code) {
        try {
            Response<TokenResponse> response = oktaService.token(
                SecretsStorage.oktaCompanyId,
                SecretsStorage.oktaClientId,
                SecretsStorage.portalHost,
                "authorization_code",
                SecretsStorage.oktaCodeVerifier,
                code
            ).execute();

            if (!response.isSuccessful()) {
                Assert.fail("[TEST] OKTA.token failed:" + response.errorBody().string());
            }

            LOGGER.info("[TEST] OKTA auth token: {}", response.body().accessToken);
            return "Bearer " + response.body().accessToken;
        } catch (IOException e) {
            Assert.fail("[TEST] Cannot reach the backend");
        }
        return null;
    }
}
