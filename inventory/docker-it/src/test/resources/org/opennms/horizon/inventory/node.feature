Feature: Node

  Background: Common Test Setup
    Given [Node] External GRPC Port in system property "application-external-grpc-port"
    Given [Node] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Node] Grpc TenantId "node-tenant-stream"
    Given [Node] Create Grpc Connection for Inventory
    Given [Common] Create "Default" Location

  Scenario: Add a node and verify list nodes by node label search returns result
    Given a new node with label "node-label", ip address "127.0.0.1" in location named "Default"
    Then verify that a new node is created with label "node-label", ip address "127.0.0.1" and location "Default"
    Then fetch a list of nodes by node label with search term "node"
    Then verify the list of nodes has size 1 and labels contain "node"
    Then verify node topic has 2 messages with tenant "node-tenant-stream"

  Scenario: Add a node and verify list nodes by node label search does not return result
    Given a new node with label "node-label", ip address "127.0.0.1" in location named "Default"
    Then verify that a new node is created with label "node-label", ip address "127.0.0.1" and location "Default"
    Then fetch a list of nodes by node label with search term "INVALID-SEARCH-TERM"
    Then verify the list of nodes is empty
