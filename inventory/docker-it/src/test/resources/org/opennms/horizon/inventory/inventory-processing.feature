Feature: Inventory Processing

  Background: Common Test Setup
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given Grpc TenantId "tenant-stream"
    #Given Grpc location named "MINION"
    Given Create Grpc Connection for Inventory
    Given [Common] Create "MINION" Location
    Given [Common] Create "MINION-2" Location
    Given [Common] Create "MINION-D" Location

  Scenario: Send an Heartbeat Message to Inventory and verify Minion and location are added
    Given Minion at location named "MINION" with system ID "MINION-TEST-1"
    Then send heartbeat message to Kafka topic "heartbeat"
    Then verify Monitoring system is created with system id "MINION-TEST-1" with location named "MINION"
    Given Minion at location named "MINION-2" with system ID "MINION-TEST-1"
    Then send heartbeat message to Kafka topic "heartbeat"
    Then verify Monitoring system is created with system id "MINION-TEST-1" with location named "MINION-2"

  Scenario: Add a device with existing location and verify Device and Associated Task creation
    Given Label "test-label"
    Given Device IP Address "192.168.10.1" in location named "MINION"
    Given Device Task IP address = "192.168.10.1"
    Given Subscribe to kafka topic "task-set-publisher"
    Then add a new device
    Then verify the device has an interface with the given IP address
    Then verify the new node return fields match
    Then retrieve the list of nodes from Inventory
    Then verify that the new node is in the list returned from inventory
    Then verify the task set update is published for device with nodeScan within 30000ms
    Then shutdown kafka consumer


  Scenario: Add a device with new location and verify that Device and location gets created
    Given add a new device with label "test-label-2" and ip address "192.168.20.1" and location named "MINION-2"
    Then verify that a new node is created with location named "MINION-2" and ip address "192.168.20.1"
    Then verify Monitoring location is created with location "MINION-2"
    Then verify adding existing device with label "test-label-2" and ip address "192.168.20.1" and location "MINION-2" will fail


  Scenario: Detection of a Device causes Monitoring and Collector Task Definitions to be Published
    Given Device IP Address "192.168.30.1" in location named "MINION"
    Given Minion at location named "MINION" with system ID "MINION-TEST-1"
    Given Device detected indicator = "true"
    Given Device Task IP address = "192.168.30.1"
    Given Device detected reason = "useful detection reason - maybe responded to ICMP"
    # SNMP has both monitor and collector tasks
    Given Monitor Type "SNMP"
    Given Subscribe to kafka topic "task-set-publisher"
    Then add a new device with label "test-label" and ip address "192.168.30.1" and location named "MINION"
    Then lookup node with location "MINION" and ip address "192.168.30.1"
    Then send Device Detection to Kafka topic "task-set.results" for an ip address "192.168.30.1" at location "MINION"
    Then verify the task set update is published for device with task suffix "icmp-monitor" within 30000ms
    Then verify the task set update is published for device with task suffix "snmp-monitor" within 30000ms
    Then verify the task set update is published for device with task suffix "snmp-collector" within 30000ms
    Then shutdown kafka consumer


  Scenario: Deletion of a device causes Task Definitions Removals to be Requested
    Given Device IP Address "192.168.30.1" in location named "MINION"
    Given Device Task IP address = "192.168.30.1"
    Given Subscribe to kafka topic "task-set-publisher"
    Then remove the device
    Then verify the task set update is published with removal of task with suffix "icmp-monitor" within 30000ms
    Then verify the task set update is published with removal of task with suffix "snmp-monitor" within 30000ms
    Then verify the task set update is published with removal of task with suffix "snmp-collector" within 30000ms
    Then shutdown kafka consumer

  @node-scan-interfaces
  Scenario: Validate Node Scan processing adds IpInterfaces SnmpInterfaces and SystemInfo
    Given Minion at location named "MINION" with system ID "MINION-TEST-1"
    Then Add a device with IP address = "192.168.1.1" with label "test-label"
    Then verify the device has an interface with the IpAddress "192.168.1.1"
    Given Node Scan results with IpInterfaces "192.168.1.45" and SnmpInterfaces with ifName "eth0"
    Then  Send node scan results to kafka topic "task-set.results"
    Then verify node has IpInterface "192.168.1.45" and SnmpInterface with ifName "eth0"
    Then verify node has SnmpInterface with ifName "et"
    Given Node Scan results with IpInterfaces "192.168.1.48" and hostName "Local"
    Then Send node scan results to kafka topic for hostName and ipAddress "task-set.results"
    Then verify node has IpInterface with hostName "Loc"
    Then verify node has IpInterface with ipAddress "192.168.1.48"


  Scenario: Validate Discovery Scan processing adds discovery id to node
    Given Minion at location named "MINION-D" with system ID "MINION-TEST-2"
    Given New Active Discovery "stream-snmp" with IpAddress "192.168.1.44" and SNMP community as "stream-snmp" at location named "MINION-D"
    Then create Active Discovery and validate it's created active discovery with given details.
    Given Discovery Scan results with IpAddress "192.168.1.44"
    Then Send discovery scan results to kafka topic "task-set.results" with location "MINION-D"
    Then verify that node is created for "192.168.1.44" and location named "MINION-D" with discoveryId
