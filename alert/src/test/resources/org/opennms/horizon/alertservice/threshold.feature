Feature: Alert Service Thresholding Functionality
  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"
    Given A new tenant
    And A monitoring policy named "my-policy" with tag "tag1"
    And The policy has a rule named "my-rule" with component type "NODE" and trap definitions
      | trigger_event_name | count | overtime | overtime_unit | severity | clear_event_name |
      | SNMP Link Down     | 2     | 0        | MINUTE        | MAJOR    |                  |
      | SNMP Link Up       | 1     | 0        | MINUTE        | CLEARED  | SNMP Link Down   |
      | SNMP Cold Start    | 3     | 10       | MINUTE        | MAJOR    |                  |
      | SNMP Warm Start    | 1     | 0        | MINUTE        | CLEARED  |                  |
    And The policy is created in the tenant

  Scenario: When a thresholding event is received from Kafka, a new alert is only created on passing the threshold
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 10
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 10
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 10
    Then List alerts for the tenant, with timeout 15000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    Then Verify alert topic has 1 messages for the tenant

  Scenario: When a thresholding event is received from Kafka, a new alert is only created on passing the threshold within the timeframe
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 10 with produced time 15 MINUTES ago
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 10 with produced time 7 MINUTES ago
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 10 with produced time 3 MINUTES ago
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 10 with produced time 2 MINUTES ago
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    Then Verify alert topic has 1 messages for the tenant

  Scenario: When a thresholding event with no time limit is received from Kafka, a new alert is only created on passing the threshold
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, with timeout 15000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    Then Verify alert topic has 1 messages for the tenant


  Scenario: A thresholded alert can be cleared by other events
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Up" on node 10
    Then List alerts for the tenant, with timeout 15000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1            |
      | alerts[0].counter == 2        |
      | alerts[0].severity == CLEARED |

  Scenario: A thresholded alert can be cleared by other events, and then recreated
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then Verify alert topic has 0 messages for the tenant
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Up" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1            |
      | alerts[0].counter == 2        |
      | alerts[0].severity == CLEARED |
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 2            |
      | alerts[0].counter == 2        |
      | alerts[0].severity == CLEARED |
      | alerts[1].counter == 1        |
      | alerts[1].severity == MAJOR   |
