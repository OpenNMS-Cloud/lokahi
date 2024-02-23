@Alert
Feature: Alert Service Basic Functionality

  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"
    Given Kafka tag topic "tag-operation"
    Given A new tenant
    And A monitoring policy named "my-policy" with tag "tag1"
    And The policy has a rule named "my-rule" with component type "NODE" and trap definitions
      | trigger_event_name | count | overtime | overtime_unit | severity | clear_event_name |
      | SNMP Link Down     | 1     | 0        | MINUTE        | MINOR    |                  |
      | SNMP Link Up       | 1     | 0        | MINUTE        | CLEARED  | SNMP Link Down   |
      | SNMP Cold Start    | 1     | 0        | MINUTE        | MAJOR    |                  |
      | SNMP Warm Start    | 1     | 0        | MINUTE        | CRITICAL |                  |
    And The policy is created in the tenant
    And Tag operations are applied to the tenant
      | action     | name | node_ids |
      | ASSIGN_TAG | tag1 | 10       |
    Then Verify list tag with size 1 and node ids
      | 10 |

  Scenario: When an event is received from Kafka, a new alert is created
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, with timeout 15000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 1 messages for the tenant
    Then Verify valid monitoring policy ID is set in alert for the tenant

  Scenario: When an event is received from Kafka, with no matching alert configuration, no new alert is created
    When An event is sent with UEI "uei.opennms.org/perspective/nodes/nodeLostService" on node 10
    Then Send GET request to application at path "/actuator/metrics/events_without_alert_data_counter", with timeout 10000ms, until JSON response matches the following JSON path expressions
      | measurements[0].value == 1.0 |
    Then Verify alert topic has 0 messages for the tenant

  Scenario: When an event is received from Kafka with a different tenant id, you do not see the new alert
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1     |
      | alerts[0].counter == 1 |
    Then List alerts for tenant "tenant-other", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 0 |

  Scenario: Alerts can be deleted
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1     |
      | alerts[0].counter == 1 |
    Then Remember the first alert from the last response
    Then Delete the alert
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 0 |

  Scenario: Alerts can be cleared by other events
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Up" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1            |
      | alerts[0].counter == 2        |
      | alerts[0].severity == CLEARED |

  Scenario: Alerts can be cleared by other events, and then recreated
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Up" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1            |
      | alerts[0].counter == 2        |
      | alerts[0].severity == CLEARED |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 2            |
      | alerts[0].counter == 2        |
      | alerts[0].severity == CLEARED |
      | alerts[1].counter == 1        |
      | alerts[1].severity == MINOR   |

  Scenario: Alerts can be acknowledged and unacknowledged
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Remember the first alert from the last response
    Then Acknowledge the alert
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1               |
      | alerts[0].counter == 1           |
      | alerts[0].isAcknowledged == true |
      | alerts[0].ackUser == me          |
      | alerts[0].ackTimeMs as long > 0  |
    Then Unacknowledge the alert
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1                |
      | alerts[0].counter == 1            |
      | alerts[0].isAcknowledged == false |
      | alerts[0].ackTimeMs == 0          |

  Scenario: Duplicate events reduce to a single alert
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 1 messages for the tenant
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 2      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 2 messages for the tenant

  Scenario: Alert responses should be paged
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant with page size 1, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 11
    Then List alerts for the tenant with page size 1, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 2 messages for the tenant

  Scenario: Alert responses can be sorted by id
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, with timeout 10000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 11
    Then List alerts for the tenant sorted by "id" ascending "true", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Verify alert topic has 2 messages for the tenant

  Scenario: Alert responses can be filtered by severity
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant filtered by severity "MINOR", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 11
    Then List alerts for the tenant filtered by severity "MAJOR", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Warm_Start" on node 12
    Then List alerts for the tenant filtered by severity "MAJOR" and "MINOR", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Verify alert topic has 3 messages for the tenant

  Scenario: Alert responses can be filtered by time
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10 with produced time 23 HOURS ago
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 11
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 12 with produced time 8 DAYS ago
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 13 with produced time 30 DAYS ago
    Then List alerts for the tenant with hours 168, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then List alerts for the tenant today, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MAJOR |
    Then List alerts for the tenant with hours 24, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Verify alert topic has 4 messages for the tenant

  Scenario: Alerts can be counted
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 10
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    When An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 11
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 2          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
      | alerts[1].severity == MAJOR |
    Then Count alerts for the tenant, assert response is 2
    Then Count alerts for the tenant, filtered by severity "MAJOR", assert response is 1
    Then Count alerts for the tenant on node 11 with page 0 pageSize 10 response is not equal to 0
    Then Verify alert topic has 2 messages for the tenant
