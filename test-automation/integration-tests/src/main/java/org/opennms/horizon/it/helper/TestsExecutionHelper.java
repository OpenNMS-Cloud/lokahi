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
package org.opennms.horizon.it.helper;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.opennms.horizon.it.GQLQueryConstants;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.querywrappers.FindAllLocationsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import javax.ws.rs.core.HttpHeaders;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

import static org.opennms.horizon.it.InventoryTestSteps.DEFAULT_HTTP_SOCKET_TIMEOUT;

public class TestsExecutionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TestsExecutionHelper.class);

    //========================================
    // Variables
    //----------------------------------------
    private Supplier<String> userAccessTokenSupplier;
    private Supplier<String> ingressUrlSupplier;
    private Supplier<String> minionImageNameSupplier;
    private Supplier<String> nodeImageNameSupplier;
    private Supplier<String> minionIngressSupplier;
    private Supplier<Integer> minionIngressPortSupplier;
    private Supplier<Boolean> minionIngressTlsSupplier;
    private Supplier<File> minionIngressCaCertificateSupplier;
    private Supplier<String> minionIngressOverrideAuthority;

    private Supplier<Network> commonNetworkSupplier;


    //========================================
    // Getters and Setters
    //----------------------------------------
    public Supplier<String> getUserAccessTokenSupplier() {
        return userAccessTokenSupplier;
    }

    public void setUserAccessTokenSupplier(Supplier<String> userAccessTokenSupplier) {
        this.userAccessTokenSupplier = userAccessTokenSupplier;
    }

    public Supplier<String> getIngressUrlSupplier() {
        return ingressUrlSupplier;
    }

    public void setIngressUrlSupplier(Supplier<String> ingressUrlSupplier) {
        this.ingressUrlSupplier = ingressUrlSupplier;
    }

    public Supplier<String> getMinionImageNameSupplier() {
        return minionImageNameSupplier;
    }

    public void setMinionImageNameSupplier(Supplier<String> minionImageNameSupplier) {
        this.minionImageNameSupplier = minionImageNameSupplier;
    }

    public void setCommonNetworkSupplier(Supplier<Network> networkSupplier) {
        commonNetworkSupplier = networkSupplier;
    }

    public Supplier<Network> getCommonNetworkSupplier() {
        return commonNetworkSupplier;
    }

    public void setNodeImageNameSupplier(Supplier<String> nodeImageNameSupplier) {
        this.nodeImageNameSupplier = nodeImageNameSupplier;
    }

    public Supplier<String> getNodeImageNameSupplier() {
        return nodeImageNameSupplier;
    }

    public Supplier<String> getMinionIngressSupplier() {
        return minionIngressSupplier;
    }

    public void setMinionIngressSupplier(Supplier<String> minionUrlSupplier) {
        this.minionIngressSupplier = minionUrlSupplier;
    }

    public Supplier<Integer> getMinionIngressPortSupplier() {
        return minionIngressPortSupplier;
    }

    public void setMinionIngressPortSupplier(Supplier<Integer> minionIngressPortSupplier) {
        this.minionIngressPortSupplier = minionIngressPortSupplier;
    }

    public Supplier<Boolean> getMinionIngressTlsEnabledSupplier() {
        return minionIngressTlsSupplier;
    }

    public void setMinionIngressTlsEnabledSupplier(Supplier<Boolean> minionIngressTlsSupplier) {
        this.minionIngressTlsSupplier = minionIngressTlsSupplier;
    }

    public Supplier<File> getMinionIngressCaCertificateSupplier() {
        return minionIngressCaCertificateSupplier;
    }

    public void setMinionIngressCaCertificateSupplier(Supplier<File> minionCaCertificateSupplier) {
        this.minionIngressCaCertificateSupplier = minionCaCertificateSupplier;
    }

    public Supplier<String> getMinionIngressOverrideAuthority() {
        return minionIngressOverrideAuthority;
    }

    public void setMinionIngressOverrideAuthority(Supplier<String> minionIngressOverrideAuthority) {
        this.minionIngressOverrideAuthority = minionIngressOverrideAuthority;
    }

    //========================================
    // Additional methods
    //----------------------------------------
    public Response executePost(URL url, String accessToken, Object body) {
        RestAssuredConfig restAssuredConfig = createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig)
            ;

        Response restAssuredResponse =
            requestSpecification
                .header(HttpHeaders.AUTHORIZATION, formatAuthorizationHeader(accessToken))
                .header(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .body(body)
                .post(url)
                .thenReturn()
            ;

        return restAssuredResponse;
    }

    public String formatAuthorizationHeader(String token) {
        return "Bearer " + token;
    }

    public RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation("SSL"))
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    public URL formatIngressUrl(String path) throws MalformedURLException {
        String baseUrl = ingressUrlSupplier.get();

        return new URL(new URL(baseUrl), path);
    }

    /**
     * Checks to see if the response contains errors
     * @param response The Response from a gqlquery
     * @return boolean True if there are errors in the response
     */
    public boolean responseContainsErrors(Response response) {
        List<Object> errorList = response.jsonPath().getList("errors");
        return (errorList != null && errorList.size() > 0);
    }

    /**
     * Method of query execution that has url and user token and need only query to run
     * @param gqlQuery GQLQuery to execute
     * @return RestAssured Response or null in case of failure
     */
    public Response executePostQuery(GQLQuery gqlQuery) {
        LOG.info("Executing GraphQL request {} with variables {}", gqlQuery.getQuery(), gqlQuery.getVariables());

        try {
            URL url = formatIngressUrl("/api/graphql");
            String accessToken = getUserAccessTokenSupplier().get();

            Response response = executePost(url, accessToken, gqlQuery);
            LOG.info("Response from server: {}", response.asPrettyString());
            return response;

        } catch (MalformedURLException e) {
            LOG.error("checkTheStatusOfTheNode failed: " + e.getMessage());
        }
        return null;
    }

    public FindAllLocationsData commonQueryLocations() throws MalformedURLException {
        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.LIST_LOCATIONS_QUERY);
        Response restAssuredResponse = executePostQuery(gqlQuery);
        Assert.assertEquals(200, restAssuredResponse.getStatusCode());
        return restAssuredResponse.getBody().as(FindAllLocationsData.class);
    }

}
