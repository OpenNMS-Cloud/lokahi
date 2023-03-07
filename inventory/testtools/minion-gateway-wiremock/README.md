# Notes

## Quick Usage:

**Maven**

    <dependency>
        <groupId>org.opennms.horizon.inventory</groupId>
        <artifactId>minion-gateway-wiremock-api</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>


**CucumberRunner**

    # Older Junit
	@CucumberOptions(
	    glue = {"org.opennms.horizon.inventory,org.opennms.horizon.testtool.miniongateway.wiremock.client", ...}

    # Newer Junit
    @ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.opennms.horizon.inventory,org.opennms.horizon.testtool.miniongateway.wiremock.client")

**Cucumber Feature File**

    Background: Configure base URLs
      Given MOCK Minion Gateway Base URL in system property "mock-minion-gateway.rest-url"

See `MinionGatewayWiremockTestSteps.java` for more step definitions.
Also note the Intellij Gherkin plugin is excellent for navigation and creation of template step definitions from the features file (Gherkin format).


## Modules

There are 3 modules here:

* api - model objects and interfaces for the mock
* client - library that can be used to communicate with the mock to define rules and send queries
* main - main, standalone, executable jar file that runs the mock


## Test Dependencies

The client module is a test tool.
As such, it directly depends on tools such as junit as runtime dependencies.
The intended usage of this module in downstream projects is with "<scope>test</scope>"
