Feature: Monitor policy gRPC Functionality

  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"

  @Dev
  Scenario: Create a monitor policy with SNMP Trap event rule
    Given Monitor policy name "test-policy" and memo "the test policy"
    Given Policy tags
      | SNMP Trap        |
      | Default location |
    Given Notify by email "true"
    Given Policy Rule name "snmp rule" and componentType "NODE"
    Given Trigger event "COLD_REBOOT", count 3 overtime 5 "MINUTE", severity "MAJOR"
    Then Create a new policy with give parameters
    Then Verify the new policy has been created
    Then List policy should contain 1


