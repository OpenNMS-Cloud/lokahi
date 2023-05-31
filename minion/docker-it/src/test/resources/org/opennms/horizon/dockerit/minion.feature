Feature: Minion Basic Functionality

  Background: Configure base URLs
    Given MOCK Minion Gateway Base URL in system property "mock-miniongateway.base-url"
    Given Application Base URL in system property "application.base-url"
    Given Application Host Name in system property "application.host-name"
    Given Netflow Listener Port in system property "netflow-5-listener-port"

  Scenario: Verify on startup the Minion has no tasks deployed
    Then Send GET request to application at path "/ignite-worker/service-deployment/metrics?verbose=true" until success with timeout 60000ms
    Then DEBUG dump the response body
    Then parse the JSON response
    Then verify JSON path expressions match
      | total == 0 |
      | serviceCount == 0 |

  Scenario: Wait for Minion to register with the Mock Gateway
    Then MOCK wait for minion connection with id "test-minion-001", timeout after 30000ms

  Scenario: Add a task to the Minion (via the Minion Gateway) and verify the task is deployed
    Then Send GET request to application at path "/ignite-worker/service-deployment/metrics?verbose=true" until success with timeout 60000ms
    Then Remember response body for later comparison

    Given MOCK twin update in resource file "/testdata/task-set.twin.001.json"
    Then MOCK send twin update for topic "task-set"

    Then Send GET request to application at path "/ignite-worker/service-deployment/metrics?verbose=true" until response changes with timeout 60000ms

    Then DEBUG dump the response body
    Then parse the JSON response
    Then verify JSON path expressions match
      | serviceCount == 1 |

  Scenario: Add another task to the Minion (via the Minion Gateway) and verify the task is deployed
    Then Send GET request to application at path "/ignite-worker/service-deployment/metrics?verbose=true"
    Then Remember response body for later comparison

    Given MOCK twin update in resource file "/testdata/task-set.twin.002.json"
    Then MOCK send twin update for topic "task-set"

    Then Send GET request to application at path "/ignite-worker/service-deployment/metrics?verbose=true" until response changes with timeout 60000ms
    Then DEBUG dump the response body
    Then parse the JSON response
    Then verify JSON path expressions match
      | serviceCount == 2 |

  Scenario: Configure Minion for Flows and send Flow package to Minion
    Given MOCK twin update in resource file "/testdata/task-set.flows.001.json"
    Then MOCK send twin update for topic "task-set"
    Then delay 5000ms
    Then Send net flow package
    Then Verify gateway has received netflow packages

  Scenario: Interrupt Minion Connection and recover data
    Given MOCK twin update in resource file "/testdata/task-set.flows.001.json"
    Then MOCK send twin update for topic "task-set"
    Then delay 5000ms

    Then Send 2 net flow package
    # The two flows are send in a single package
    Then Verify gateway has received 1 netflow packages

    Then Interrupt minion connection
    Then delay 1000ms
    Then Send 3 net flow package

    # Verify that we have not received the other flows somehow but give it enough time for the aggregator to be flushed
    Then delay 2000ms
    Then Verify gateway has received 1 netflow packages

    Then Restore minion connection
    # The three flows are stuffed in the second package
    Then Verify gateway has received 2 netflow packages

    Then Send 2 net flow package
    # Connection is fully restored and we receive the last two flows in a third package
    Then Verify gateway has received 3 netflow packages
