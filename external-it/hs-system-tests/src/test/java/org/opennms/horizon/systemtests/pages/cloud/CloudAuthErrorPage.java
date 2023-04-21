package org.opennms.horizon.systemtests.pages.cloud;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class CloudAuthErrorPage {
    private final static SelenideElement errorMessage = $("#hs-content-wrapper");

    public static void verifyAuthError(String userEmail) {
        errorMessage.shouldHave(
            text(String.format("User %s authenticated with identity provider okta does not exist. Please contact your administrator.", userEmail)), Duration.ofSeconds(30)
        );
    }

    public static void logout() {
        Selenide.open("https://opennms.oktapreview.com/login/signout"); // https://opennms.atlassian.net/browse/BTO-280
        CloudLoginPage.checkPageTitle();
    }
}
