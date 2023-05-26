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
package org.opennms.horizon.it;

import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.LocationData;
import org.opennms.horizon.it.gqlmodels.querywrappers.AddDiscoveryResult;
import org.opennms.horizon.it.gqlmodels.querywrappers.CreateNodeResult;
import org.opennms.horizon.it.helper.TestsExecutionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiscoveryTestSteps {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryTestSteps.class);

    private TestsExecutionHelper helper;

    public DiscoveryTestSteps(TestsExecutionHelper helper) {
        this.helper = helper;
    }

//========================================
// Test Step Definitions
//----------------------------------------

    /**
     * This test step is to create a new discovery
     * @param name Name of the discovery
     * @param location Rather Default or behind the Minion
     * @param ipaddress Ip address or range of addresses separated by -
     * @param port port or array of ports separated by comma
     * @param communities Community string
     * @throws MalformedURLException
     */
    @Then("Add a new active discovery for the name {string} at location {string} with ip address {string} and port {int}, readCommunities {string}")
    public void addANewActiveDiscovery(String name, String location, String ipaddress, int port, String communities) throws MalformedURLException {
        LOG.info("Add a new discovery query execution steps");

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> location.equals(loc.getLocation()))
            .findFirst()
            .map(LocationData::getId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));
        String query = String.format(GQLQueryConstants.ADD_DISCOVERY_QUERY, name, locationId, ipaddress, communities, port);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("add-discovery query failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
            200, response.getStatusCode());

        AddDiscoveryResult discoveryResult = response.getBody().as(AddDiscoveryResult.class);

        // GRAPHQL errors result in 200 http response code and a body with "errors" detail
        assertTrue("create-node errors: " + discoveryResult.getErrors(),
            ( discoveryResult.getErrors() == null ) || ( discoveryResult.getErrors().isEmpty() ));
   }

}
