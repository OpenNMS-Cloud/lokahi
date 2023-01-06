Feature: Inventory Processing

  Background: Common Test Setup
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given Grpc TenantId "tenant-stream"
    Given Create Grpc Connection for Inventory

  Scenario: Send an Heartbeat Message to Inventory and verify Minion and location are added
    Given Minion at location "MINION" with system Id "MINION-TEST-1"
    Given send heartbeat message to Kafka topic "heartbeat"
    Then verify Monitoring system is created with system id "MINION-TEST-1"
    Then verify Monitoring location is created with location "MINION"

  Scenario: Add a device with existing location and verify if Device got created
    Given add a new device with label "test-label" and ip address "192.168.1.1"
    Then verify that a new node is created with label "test-label" and ip address "192.168.1.1"

  Scenario: Add a device with new location and verify that Device and location gets created
    Given add a new device with label "test-label-2" and ip address "192.168.1.2" and location "MINION-2"
    Then verify that a new node is created with location "MINION-2" and ip address "192.168.1.2"
    Then verify Monitoring location is created with location "MINION-2"

  Scenario: Add a device with existing ip address for a given location and verify that creation fails
    Then verify adding existing device with label "test-label-2" and ip address "192.168.1.2" and location "MINION-2" will fail
