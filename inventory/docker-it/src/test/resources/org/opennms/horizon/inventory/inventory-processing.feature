Feature: Inventory Processing

  Background: Common Test Setup
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given MOCK Minion Gateway Base URL in system property "mock-minion-gateway.rest-url"
    Given Grpc TenantId "tenant-stream"
    Given Create Grpc Connection for Inventory

  Scenario: Send an Heartbeat Message to Inventory and verify Minion and location are added
    Given Minion at location "MINION" with system Id "MINION-TEST-1"
    Then send heartbeat message to Kafka topic "heartbeat"
    Then verify Monitoring system is created with system id "MINION-TEST-1"
    Then verify Monitoring location is created with location "MINION"

  Scenario: Add a device with existing location and verify Device and Associated Task creation
    Given Label "test-label"
    Given New Device IP Address "192.168.1.1"
    Given Device Task IP address = "192.168.1.1"
    Then add a new device
    Then verify the device has an interface with the given IP address
    Then verify the new node return fields match
    Then retrieve the list of nodes from Inventory
    Then verify that the new node is in the list returned from inventory
    Then verify the task set update is published for device with nodeScan within 30000ms


  Scenario: Add a device with new location and verify that Device and location gets created
    Given add a new device with label "test-label-2" and ip address "192.168.1.2" and location "MINION-2"
    Then verify that a new node is created with location "MINION-2" and ip address "192.168.1.2"
    Then verify Monitoring location is created with location "MINION-2"

  Scenario: Add a device with existing ip address for a given location and verify that creation fails
    Then verify adding existing device with label "test-label-2" and ip address "192.168.1.2" and location "MINION-2" will fail


  Scenario: Detection of a Device causes Monitoring and Collector Task Definitions to be Published
    Given New Device Location "MINION"
    Given Minion at location "MINION" with system Id "MINION-TEST-1"
    Given Device detected indicator = "true"
    Given Device Task IP address = "192.168.1.1"
    Given Device detected reason = "useful detection reason - maybe responded to ICMP"
    # SNMP has both monitor and collector tasks
    Given Monitor Type "SNMP"
    Then add a new device with label "test-label" and ip address "192.168.1.1" and location "MINION"
    Then lookup node with location "MINION" and ip address "192.168.1.1"
    Then send Device Detection to Kafka topic "task-set.results" for an ip address "192.168.1.1" at location "MINION"
    Then verify the task set update is published for device with task suffix "icmp-monitor" within 30000ms
    Then verify the task set update is published for device with task suffix "snmp-monitor" within 30000ms

  
  Scenario: Deletion of a device causes Task Definitions Removals to be Requested
    Given Existing Device IP Address "192.168.1.1"
    Given Existing Device Location "MINION"
    Given Device Task IP address = "192.168.1.1"
    Then remove the device
    Then verify the task set update is published with removal of task with suffix "icmp-monitor" within 30000ms
    Then verify the task set update is published with removal of task with suffix "snmp-monitor" within 30000ms

# TBD888 - Test multi-tenancy
# TBD888 - Test Flows and Traps Configs published
