@icmp-active-discovery
Feature: Active Discovery

  Background: Common Test Setup
    Given [ICMP Discovery] External GRPC Port in system property "application-external-grpc-port"
    Given [ICMP Discovery] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given Grpc TenantId "tenant-icmp-discovery"
    Given [ICMP Discovery] Create Grpc Connection for Inventory
    Given [Common] Create "MINION" Location


  Scenario: Create Active discovery and verify task set is published
    Given New Active Discovery with IpAddresses "192.168.1.1-192.168.1.255" and SNMP community as "stream-snmp" at location named "MINION" with tags "mercury,pluto"
    Given Discovery Subscribe to kafka topic "task-set-publisher"
    Then  create Active Discovery and validate it's created active discovery with above details.
    Then  verify get active discovery with above details.
    Given The taskset for location named "MINION"
    Then verify the task set update is published for icmp discovery within 30000ms
    Then send discovery ping results for "192.168.1.17" to Kafka topic "task-set.results"
    Then verify that node is created for "192.168.1.17" and location named "MINION" with same tags within 30000ms
    Then Discovery shutdown kafka consumer

  Scenario: Create another Active discovery and verify list has two items
    Given New Active Discovery with IpAddresses "192.168.1.1-192.168.1.211" and SNMP community as "second-snmp" at location named "MINION" with tags "mars, venus"
    Then  create Active Discovery and validate it's created active discovery with above details.
    Then verify list has 2 items

  Scenario: Update Active discovery and verify task set is published
    Given New Active Discovery with IpAddresses "192.168.2.0/4" and SNMP community as "lokahi" at location named "MINION" with tags "lokahi,default"
    Given Discovery Subscribe to kafka topic "task-set-publisher"
    Then  create Active Discovery and validate it's created active discovery with above details.
    Then  verify get active discovery with above details.
    Given The taskset for location named "MINION"
    Then verify the task set update is published for icmp discovery within 30000ms
    Given Update existing Active Discovery with IpAddresses "192.168.3.0/4" and SNMP community as "default" at location named "MINION" with tags "mars,earth"
    Then  update Active Discovery and validate it's created active discovery with above details.
    Then  verify get active discovery with above details.
    Given The taskset for location named "MINION"
    Then verify the task set update is published for icmp discovery within 30000ms
    Then Discovery shutdown kafka consumer

  Scenario: Delete Active discovery and verify task set is published
    Given New Active Discovery with IpAddresses "192.168.2.0/4" and SNMP community as "lokahi" at location named "MINION" with tags "lokahi,default"
    Given Discovery Subscribe to kafka topic "task-set-publisher"
    Then  create Active Discovery and validate it's created active discovery with above details.
    Then  verify get active discovery with above details.
    Given The taskset for location named "MINION"
    Then verify the task set update is published for icmp discovery within 30000ms
    Then delete Active Discovery that is created in previous step
    Given The taskset for location named "MINION"
    Then verify the task set update for removal is published for icmp discovery within 30000ms
    Then Discovery shutdown kafka consumer
