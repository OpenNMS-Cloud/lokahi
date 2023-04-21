package org.opennms.horizon.systemtests.steps.cloud;

import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;
import org.opennms.horizon.systemtests.pages.cloud.CloudLoginPage;
import org.opennms.horizon.systemtests.utils.TestDataStorage;

public class CloudLoginSteps {

    @Then("set email address as {string}")
    public void setEmail(String email) {
        String userEmail = TestDataStorage.mapUserToEmail(email);
        CloudLoginPage.setUsername(userEmail);
    }

    @Then("click on 'Next' button")
    public void clickNextBtn() {
        CloudLoginPage.clickNextBtn();
    }

    @Then("set password")
    public void setPassword() {
        CloudLoginPage.setPassword(SecretsStorage.adminUserPassword);
    }

    @Then("click on 'Sign in' button")
    public void clickSignIn() {
        CloudLoginPage.clickSignInBtn();
    }
}

