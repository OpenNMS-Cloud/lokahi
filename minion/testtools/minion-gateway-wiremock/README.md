# Notes

## Quick Usage:

**Maven**

    <dependency>
        <groupId>org.opennms.lokahi.minion-gateway-wiremock</groupId>
        <artifactId>client</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>


**CucumberRunner**

	@CucumberOptions(
	    glue = {"org.opennms.horizon.testtool.miniongateway.wiremock.client", ...}


**Cucumber Feature File**

    Background: Configure base URLs
      Given MOCK Minion Gateway Base URL in system property "mock-miniongateway.base-url"
      Given Application Base URL in system property "application.base-url"

    Scenario: Verify on startup the Minion has no tasks deployed
      Then Send GET request to application at path "/ignite-worker/service-deployment/metrics?verbose=true"
      Then parse the JSON response
      Then verify JSON path expressions match
        | total == 0 |
        | serviceCount == 0 |



## Modules

There are 3 modules here:

* api - model objects and interfaces for the mock
* client - library that can be used to communicate with the mock to define rules and send queries
* main - main, standalone, executable jar file that runs the mock


## Test Dependencies

The client module is a test tool.
As such, it directly depends on tools such as junit as runtime dependencies.
The intended usage of this module in downstream projects is with "<scope>test</scope>"
