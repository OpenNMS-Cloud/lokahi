@Alert
Feature: Alert Service Basic Functionality
  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"
    Given Tenant id "tenantA"
    Given Monitor policy name "tenantA-policy" and memo "tenantA policy"
    Given Policy Rule name "tenantA-rule" and componentType "NODE"
    And Trigger events data
      | trigger_event   | count | overtime | overtime_unit | severity | clear_event    |
      | SNMP_Link_Down  | 1     | 0        | MINUTE        | MINOR    |                |
      | SNMP_Link_Up    | 1     | 0        | MINUTE        | CLEARED  | SNMP_Link_Down |
      | SNMP_Cold_Start | 1     | 0        | MINUTE        | MAJOR    |                |
      | SNMP_Warm_Start | 1     | 0        | MINUTE        | CRITICAL |                |
    And Create a new policy with give parameters
    Given Tenant id "tenantF"
    And Create a new policy with give parameters
    Given Tenant id "tenantG"
    And Create a new policy with give parameters
    Given Tenant id "tenantH"
    And Create a new policy with give parameters
    Given Tenant id "tenantI"
    And Create a new policy with give parameters
    Given Tenant id "tenantJ"
    And Create a new policy with give parameters
    Given Tenant id "tenantK"
    And Create a new policy with give parameters

  Scenario: Verify when an event is received from Kafka, a new alert is created
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantA" with node 10
    Then List alerts for tenant "tenantA", with timeout 15000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 1 messages with tenant "tenantA"

  Scenario: Verify when an event is received from Kafka, with no matching alert configuration, no new alert is created
    Then Send event with UEI "uei.opennms.org/perspective/nodes/nodeLostService" with tenant "tenantB" with node 10
    Then Send GET request to application at path "/metrics/events_without_alert_data_counter", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | measurements[0].value == 1.0 |
    Then Verify alert topic has 0 messages with tenant "tenantB"

  Scenario: Verify when an event is received from Kafka with a different tenant id, you do not see the new alert
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantA" with node 10
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
    Then List alerts for tenant "tenant-other", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 0 |

  Scenario: Verify that alerts can be deleted
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1   |
      | alerts[0].counter == 1 |
    Then Remember the first alert from the last response
    Then Delete the alert
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 0 |

  Scenario: Verify that alerts can be cleared by other events
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantA" with node 10
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Up" with tenant "tenantA" with node 10
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1            |
      | alerts[0].counter == 2        |
      | alerts[0].severity == CLEARED |

  Scenario: Verify alert can be acknowledged and unacknowledged
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantA" with node 10
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Remember the first alert from the last response
    Then Acknowledge the alert
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].isAcknowledged == true |
      | alerts[0].ackUser == me |
      | alerts[0].ackTimeMs as long > 0 |
    Then Unacknowledge the alert
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].isAcknowledged == false |
      | alerts[0].ackTimeMs == 0 |

  Scenario: Verify alert reduction for duplicate events
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantF" with node 10
    Then List alerts for tenant "tenantF", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 1 messages with tenant "tenantF"
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantF" with node 10
    Then List alerts for tenant "tenantF", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 2      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 2 messages with tenant "tenantF"

  Scenario: Verify page size 1 should return only 1 alert
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantG" with node 10
    Then List alerts for tenant "tenantG" with page size 1, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantG" with node 11
    Then List alerts for tenant "tenantG" with page size 1, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 2 messages with tenant "tenantG"

  Scenario: Verify find alert sorted by id
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantH" with node 10
    Then List alerts for tenant "tenantH", with timeout 10000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "tenantH" with node 11
    Then List alerts for tenant "tenantH" sorted by "id" ascending "true", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Verify alert topic has 2 messages with tenant "tenantH"

  Scenario: Verify find alert filtered by severity MAJOR and MINOR
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantI" with node 10
    Then List alerts for tenant "tenantI", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "tenantI" with node 11
    Then List alerts for tenant "tenantI" filtered by severity "MAJOR", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Warm_Start" with tenant "tenantI" with node 12
    Then List alerts for tenant "tenantI" filtered by severity "MAJOR" and "MINOR", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Verify alert topic has 3 messages with tenant "tenantI"

  Scenario: Verify find alert filtered by time
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantJ" with node 10 with produced time 23h ago
    Then List alerts for tenant "tenantJ", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "tenantJ" with node 11
    Then List alerts for tenant "tenantJ", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantJ" with node 12 with produced time 8 days ago
    Then List alerts for tenant "tenantJ", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2              |
      | alerts[0].counter == 1          |
      | alerts[0].severity == MINOR     |
      | alerts[1].severity == MAJOR     |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantJ" with node 13 with produced time last month
    Then List alerts for tenant "tenantJ" with hours 168, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2              |
      | alerts[0].counter == 1          |
      | alerts[0].severity == MINOR     |
      | alerts[1].severity == MAJOR     |
    Then List alerts for tenant "tenantJ" today , with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1              |
      | alerts[0].counter == 1          |
      | alerts[0].severity == MAJOR     |
    Then List alerts for tenant "tenantJ" with hours 24, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2              |
      | alerts[0].counter == 1          |
      | alerts[0].severity == MINOR     |
      | alerts[1].severity == MAJOR     |
    Then Verify alert topic has 4 messages with tenant "tenantJ"

    Scenario: Verify count alerts
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenantK" with node 10
    Then List alerts for tenant "tenantK", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "tenantK" with node 11
    Then List alerts for tenant "tenantK", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Count alerts for tenant "tenantK", assert response is 2
    Then Count alerts for tenant "tenantK" filtered by severity "MAJOR", assert response is 1
    Then Verify alert topic has 2 messages with tenant "tenantK"
