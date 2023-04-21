package org.opennms.horizon.systemtests.steps.cloud;

import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.cloud.CloudAuthErrorPage;
import org.opennms.horizon.systemtests.utils.TestDataStorage;

public class CloudAuthErrorSteps {
    @Then("see 'We are sorry...' error with access restriction for {string} user")
    public void verifyErrorMessage(String email) {
        String userEmail = TestDataStorage.mapUserToEmail(email);
        CloudAuthErrorPage.verifyAuthError(userEmail);
        CloudAuthErrorPage.logout();
    }

}
