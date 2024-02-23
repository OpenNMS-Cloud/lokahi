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

  Scenario: Update existing node alias
    Given a new node with label "node1", ip address "127.0.0.1" in location named "Default"
    Given a new node with label "node2", ip address "127.0.0.2" in location named "Default" without clear all
    Given update node "node1" with alias "alias1" exception "false"
    Given update node "node2" with alias "alias1" exception "true"
    Then [Node] Verify exception "StatusRuntimeException" thrown with message "INVALID_ARGUMENT: Duplicate node alias with name alias1"

  Scenario: Add a node and verify list nodes by node alias search returns result
    Given a new node with node_alias "node-alias", label "node-label", ip address "127.0.0.1" in location named "Default"
    Then verify that a new node is created with node_alias "node-alias"
    Then fetch a list of nodes by node node_alias with search term "node-alias"

  Scenario: Add a node
    Given a new node with node_alias "node", label "label", ip address "127.0.0.1" in location named "Default"
    Then fetch the list of nodes response not equal to 0
