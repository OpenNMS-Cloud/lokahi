Feature: Synthetic Transactions

  Background: Common Test Setup
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given MOCK Minion Gateway Base URL in system property "mock-minion-gateway.rest-url"
    Given Grpc TenantId "tenant-foo"
    Given Create Grpc Connection for Inventory

    Given Minion at location "London" with system Id "London#1"
    Then send heartbeat message to Kafka topic "heartbeat"
    Given Minion at location "Amsterdam" with system Id "Amsterdam#1"
    Then send heartbeat message to Kafka topic "heartbeat"
    Then verify Monitoring location is created with location "London"
    Then verify Monitoring location is created with location "Amsterdam"

  Scenario: Create new synthetic transaction
    Given Synthetic transaction label "test-transaction"
    When Synthetic transaction is created
    Then Synthetic transaction with label "test-transaction" exists
    When Synthetic transaction "test-transaction" is deleted
    Then Synthetic transaction with label "test-transaction" does not exist

  Scenario: Create new synthetic transaction test
    Given Synthetic transaction label "sample-transaction"
    When Synthetic transaction is created
    Then Synthetic transaction with label "sample-transaction" exists
    Given Synthetic transaction test associated with transaction "sample-transaction":
      | property           | value       |
      | label              | icmp        |
      | schedule           | 60000       |
      | locations          | Amsterdam   |
      | config.pluginName  | ICMPMonitor |
      | config.ipAddress   | 127.0.0.1   |
      | resilience.timeout | 1000        |
      | resilience.retries | 3           |
    When Synthetic transaction test is created
    Then Synthetic transaction "sample-transaction" contains test labelled "icmp"
    Given Monitor Type "ICMP"
    Then verify the task set update for location "Amsterdam" is published with synthetic transaction 30000ms
