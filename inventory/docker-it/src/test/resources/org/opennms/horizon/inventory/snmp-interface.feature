
Feature: SnmpInterface
  Scenario: Add a snmp_interface
    Given a new node with label "snmp-label", ip address "127.0.0.1" in location named "Default"
    Given a new snmp_interface with name "name", desc "desc" ,alias "alias" , physical address "127.0.0.1" and node label "snmp-label" with tenant "node-tenant-stream"
    Then fetch a list of snmp_interface by name with search "name"
