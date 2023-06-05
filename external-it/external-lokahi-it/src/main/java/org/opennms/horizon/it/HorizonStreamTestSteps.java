package org.opennms.horizon.it;

import io.cucumber.java.en.Given;

import java.io.File;
import org.opennms.horizon.it.helper.TestsExecutionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HorizonStreamTestSteps {

    private static final Logger log = LoggerFactory.getLogger(HorizonStreamTestSteps.class);

    private static final String DEFAULT_MINION_IMAGE_NAME = "opennms/lokahi-minion:latest";

    private final KeycloakTestSteps keycloakTestSteps;
    private final InventoryTestSteps inventoryTestSteps;
    private final MetricsTestSteps metricsTestSteps;
    private final TestsExecutionHelper testsExecutionHelper;

    private String ingressBaseUrl;
    private String minionImageName;
    private String minionIngress;
    private boolean minionIngressTlsEnabled;
    private int minionIngressPort;
    private String minionIngressCaCertificate;
    private String minionIngressOverrideAuthority;

//========================================
// Constructor
//----------------------------------------

    public HorizonStreamTestSteps(KeycloakTestSteps keycloakTestSteps, InventoryTestSteps inventoryTestSteps, MetricsTestSteps metricsTestSteps, TestsExecutionHelper testsExecutionHelper) {
        this.keycloakTestSteps = keycloakTestSteps;
        this.inventoryTestSteps = inventoryTestSteps;
        this.metricsTestSteps = metricsTestSteps;
        this.testsExecutionHelper = testsExecutionHelper;

        this.testsExecutionHelper.setUserAccessTokenSupplier(this.keycloakTestSteps::getKeycloakAccessToken);
        this.testsExecutionHelper.setIngressUrlSupplier(this::getIngressBaseUrl);
        this.testsExecutionHelper.setMinionImageNameSupplier(() -> minionImageName);
        this.testsExecutionHelper.setMinionIngressSupplier(() -> minionIngress);
        this.testsExecutionHelper.setMinionIngressPortSupplier(() -> minionIngressPort);
        this.testsExecutionHelper.setMinionIngressCaCertificateSupplier(() -> minionIngressCaCertificate != null ? new File(minionIngressCaCertificate).getAbsoluteFile() : null);
        this.testsExecutionHelper.setMinionIngressTlsEnabledSupplier(() -> minionIngressTlsEnabled);
        this.testsExecutionHelper.setMinionIngressOverrideAuthority(() -> minionIngressOverrideAuthority);

        this.metricsTestSteps.setMinionsAtLocationSupplier(this.inventoryTestSteps::getMinionsAtLocation);
    }

//========================================
// Getters and Setters
//----------------------------------------

    public String getIngressBaseUrl() {
        return ingressBaseUrl;
    }


//========================================
// Test Step Definitions
//----------------------------------------

    @Given("Ingress base url in environment variable {string}")
    public void horizonStreamServerBaseUrlInEnvironmentVariable(String variableName) {
        ingressBaseUrl = System.getenv(variableName);

        log.info("INGRESS BASE URL: {}", ingressBaseUrl);
    }

    @Given("Minion image name in environment variable {string}")
    public void minionImageNameInEnvironmentVariable(String variableName) {
        minionImageName = System.getenv(variableName);

        if (minionImageName == null) {
            log.info("Environment variable {} not defined, using default value {}", variableName, DEFAULT_MINION_IMAGE_NAME);
            minionImageName = DEFAULT_MINION_IMAGE_NAME;
        }

        log.info("MINION IMAGE NAME: {}", minionImageName);
    }

    @Given("Minion ingress base url in environment variable {string}")
    public void horizonStreamMinionIngressUrlInEnvironmentVariable(String variableName) {
        minionIngress = System.getenv(variableName);

        log.info("MINION INGRESS BASE URL: {}", minionIngress);
    }

    @Given("Minion ingress CA certificate file is in environment variable {string}")
    public void horizonStreamMinionIngressCaCertificateEnvironmentVariable(String variableName) {
        minionIngressCaCertificate = System.getenv(variableName);

        log.info("MINION INGRESS CA Certificate file: {}", minionIngressCaCertificate);
    }

    @Given("Minion ingress TLS enabled flag is in variable {string}")
    public void horizonStreamMinionIngressTlsEnabledEnvironmentVariable(String variableName) {
        minionIngressTlsEnabled = Boolean.parseBoolean(System.getenv(variableName));

        log.info("MINION INGRESS TLS: {}", minionIngressTlsEnabled);
    }

    @Given("Minion ingress port is in variable {string}")
    public void horizonStreamMinionIngressPortEnvironmentVariable(String variableName) {
        minionIngressPort = Integer.parseInt(System.getenv(variableName));

        log.info("MINION INGRESS PORT: {}", minionIngressPort);
    }

    @Given("Minion ingress overridden authority is in variable {string}")
    public void horizonStreamMinionIngressOverrideAuthorityEnvironmentVariable(String variableName) {
        minionIngressOverrideAuthority = System.getenv(variableName);

        log.info("MINION INGRESS OVERRIDDEN AUTHORITY: {}", minionIngressOverrideAuthority);
    }

}
