Feature: Alert Service Basic Functionality

  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"

  Scenario: Verify when an event is received from Kafka, a new alert is created
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantA" with node 10
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 1 messages with tenant "tenantA"

  Scenario: Verify when an event is received from Kafka, with no matching alert configuration, no new alert is created
    Then Send event with UEI "uei.opennms.org/perspective/nodes/nodeLostService" with tenant "tenantB" with node 10
    Then Send GET request to application at path "/metrics/events_without_alert_data_counter", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | measurements[0].value == 1.0 |
    Then Verify alert topic has 0 messages with tenant "tenantB"

  Scenario: Verify when an event is received from Kafka with a different tenant id, you do not see the new alert
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantA" with node 10
    Then List alerts for tenant "tenantA", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
    Then List alerts for tenant "tenant-other", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 0 |

  Scenario: Verify that alerts can be deleted
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantC" with node 10
    Then List alerts for tenant "tenantC", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
    Then Remember the first alert from the last response
    Then Delete the alert
    Then List alerts for tenant "tenantC", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 0 |

  Scenario: Verify that alerts can be cleared by other events
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantD" with node 10
    Then List alerts for tenant "tenantD", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Up" with tenant "tenantD" with node 10
    Then List alerts for tenant "tenantD", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 2 |
      | alerts[0].severity == CLEARED |

  Scenario: Verify alert can be acknowledged and unacknowledged
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantE" with node 10
    Then List alerts for tenant "tenantE", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MINOR |
    Then Remember the first alert from the last response
    Then Acknowledge the alert
    Then List alerts for tenant "tenantE", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].isAcknowledged == true |
      | alerts[0].ackUser == me |
      | alerts[0].ackTimeMs as long > 0 |
    Then Unacknowledge the alert
    Then List alerts for tenant "tenantE", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].isAcknowledged == false |
      | alerts[0].ackTimeMs == 0 |

  Scenario: Verify alert reduction for duplicate events
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantF" with node 10
    Then List alerts for tenant "tenantF", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 1 messages with tenant "tenantF"
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantF" with node 10
    Then List alerts for tenant "tenantF", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 2 |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 2 messages with tenant "tenantF"

  Scenario: Verify page size 1 should return only 1 alert
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantG" with node 10
    Then List alerts for tenant "tenantG" with page size 1, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MINOR |
    Then Send event with UEI "uei.opennms.org/vendor/cisco/traps/SNMP_Link_Down" with tenant "tenantG" with node 11
    Then List alerts for tenant "tenantG" with page size 1, with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1          |
      | alerts[0].counter == 1      |
      | alerts[0].severity == MINOR |
    Then Verify alert topic has 2 messages with tenant "tenantG"
