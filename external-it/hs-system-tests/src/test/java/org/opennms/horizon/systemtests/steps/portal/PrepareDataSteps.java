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

package org.opennms.horizon.systemtests.steps.portal;

import io.cucumber.java.en.Given;
import org.opennms.horizon.systemtests.CucumberHooks;
import org.opennms.horizon.systemtests.api.portal.models.BtoInstancesResponse;
import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

import static org.opennms.horizon.systemtests.CucumberHooks.INSTANCES;
import static org.opennms.horizon.systemtests.CucumberHooks.portalApi;

public class PrepareDataSteps {

    @Given("No BTO instances created")
    public void emptyState() {
        portalApi.deleteAllBtoInstances();
    }

    @Given("BTO instance name {string} created")
    public void prepareAnInstance(String instanceName) {
        if (instanceName.startsWith("random")) {
            instanceName = "Instance_" + RandomStringUtils.randomAlphabetic(10);
            INSTANCES.add(instanceName);
        }
        CucumberHooks.portalApi.createBtoInstance(instanceName, SecretsStorage.adminUserEmail);
    }

    @Given("A list of BTO instances are created")
    public void prepareAnInstance(List<String> instanceNames) {
        instanceNames.forEach(name ->
            CucumberHooks.portalApi.createBtoInstance(name, SecretsStorage.adminUserEmail)
        );
    }

    @Given("someone deletes the {string} instance")
    public void deleteInstanceByName(String instanceName) {
        BtoInstancesResponse allBtoInstancesByName = CucumberHooks.portalApi.getAllBtoInstancesByName(instanceName);
        portalApi.deleteBtoInstance(allBtoInstancesByName.pagedRecords.get(0).id);
    }
}
