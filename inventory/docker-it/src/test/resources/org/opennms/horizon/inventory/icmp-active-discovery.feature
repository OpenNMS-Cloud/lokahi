Feature: Active Discovery

  Background: Common Test Setup
    Given [ICMP Discovery] External GRPC Port in system property "application-external-grpc-port"
    Given [ICMP Discovery] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given MOCK Minion Gateway Base URL in system property "mock-minion-gateway.rest-url"
    Given Grpc TenantId "tenant-icmp-discovery"
    Given [ICMP Discovery] Create Grpc Connection for Inventory


  Scenario: Create Active discovery and verify task set is published
    Given New Active Discovery with IpAddresses "192.168.1.1-192.168.1.255" and SNMP community as "stream-snmp" at location "MINION" with tags "mercury,pluto"
    Then  create Active Discovery and validate it's created active discovery with above details.
    Then  verify get active discovery with above details.
    Given The taskset at location "MINION"
    Then verify the task set update is published for icmp discovery within 30000ms
    Then send discovery ping results for "192.168.1.17" to Kafka topic "task-set.results"
    Then verify that node is created for "192.168.1.17" and location "MINION" with same tags within 30000ms

  Scenario: Create another Active discovery and verify list has two items
    Given New Active Discovery with IpAddresses "192.168.1.1-192.168.1.211" and SNMP community as "second-snmp" at location "MINION" with tags "mars, venus"
    Then  create Active Discovery and validate it's created active discovery with above details.
    Then verify list has 2 items
