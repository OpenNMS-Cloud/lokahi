package org.opennms.horizon.systemtests.steps.portal;

import io.cucumber.java.en.Given;
import org.opennms.horizon.systemtests.CucumberHooks;
import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import static org.opennms.horizon.systemtests.CucumberHooks.INSTANCES;

public class PrepareDataSteps {
    @Given("BTO instance name {string} created")
    public void prepareAnInstance(String instanceName) {
        if (instanceName.startsWith("random")) {
            instanceName = "Instance_" + RandomStringUtils.randomAlphabetic(10);
            INSTANCES.add(instanceName);
        }
        CucumberHooks.portalApi.createBtoInstance(instanceName, SecretsStorage.adminUserEmail);
    }
}
