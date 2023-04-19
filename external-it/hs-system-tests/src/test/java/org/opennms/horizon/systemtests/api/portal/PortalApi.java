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
import org.opennms.horizon.systemtests.api.portal.models.BtoInstanceRequest;
import org.opennms.horizon.systemtests.api.portal.models.BtoInstancesResponse;
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
    public String createBtoInstance(String instanceName, String adminEmail) {
        try {
            Response<ResponseBody> btoInstanceId = portalService.createBtoInstance(
                SecretsStorage.portalOrganizationId,
                new BtoInstanceRequest(instanceName, adminEmail),
                authToken
            ).execute();

            if (btoInstanceId.isSuccessful()) {
                return btoInstanceId.body().string();
            }
            Assert.fail("[TEST] Create a bto instance request failed: " + btoInstanceId.errorBody().string());
        } catch (IOException ex) {
            Assert.fail("[TEST] Portal.createBtoInstance request failed: " + ex.getMessage());
        }
        return null;
    }

    public BtoInstancesResponse getAllBtoInstances() {
        try {
            Response<BtoInstancesResponse> instances = portalService.getBtoInstances(
                SecretsStorage.portalOrganizationId,
                authToken
            ).execute();

            if (!instances.isSuccessful()) {
                Assert.fail("[TEST] portalService.getBtoInstances failed with error " + instances.errorBody().string());
            }
            return instances.body();
        } catch (IOException e) {
            Assert.fail("[TEST] Test failed getting the bto instances: " + e.getLocalizedMessage());
        }
        return null;
    }

    public void deleteBtoInstance(String btoInstanceId) {
        try {
            Response<Void> deleteResponse = portalService.deleteInstance(
                SecretsStorage.portalOrganizationId,
                btoInstanceId,
                authToken
            ).execute();

            if (!deleteResponse.isSuccessful()) {
                Assert.fail("[TEST] portalService.deleteInstance failed with error " + deleteResponse.errorBody().string());
            }
        } catch (IOException e) {
            Assert.fail("[TEST] Test failed deleting the bto instances: " + e.getLocalizedMessage());
        }
    }

    public void deleteAllBtoInstances() {
        List<BtoInstancesResponse.Instance> items = getAllBtoInstances().pagedRecords;

        for (BtoInstancesResponse.Instance item : items) {
            deleteBtoInstance(item.id);
            LOGGER.info("[TEST] BTO instance {} deleted", item.name);
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
