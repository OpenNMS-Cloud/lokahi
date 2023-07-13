Feature: Monitored State

  Background: Common Test Setup
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given Grpc TenantId "tenant-stream"
    Given Grpc location named "MINION"
    Given Create Grpc Connection for Inventory
    Given [Common] Create "Belfast" Location
    Given [Tags] Nodes are associated with location named "Belfast"
    Given [Tags] A clean system
    Given [PassiveDiscovery] A clean system
    Given [ActiveDiscovery] A clean system

  @ignore
  Scenario: Discover a node so that it's monitored state is "MONITORED"
    Given Passive discovery tags "my-new-tag"
    Given A new monitoring policy with tags "my-new-tag"

    When A new node with tags "my-new-tag"

    Then The monitored state will be "MONITORED"

  @ignore
  Scenario: Change a "MONITORED" node so that it's state becomes "UNMONITORED"
    Given Passive discovery tags "my-new-tag"
    Given A new monitoring policy with tags "my-new-tag"
    Given A new node with tags "my-new-tag,my-other-tag"
    Given The monitored state will be "MONITORED"

    When A GRPC request to remove tag "my-new-tag" for node

    Then The monitored state will be "UNMONITORED"

  Scenario: Change a "DETECTED" node so that it's state becomes "MONITORED"
    Given Passive discovery tags "my-new-tag"
    Given A new node with tags "my-new-tag"
    Given The monitored state will be "DETECTED"

    When A new monitoring policy with tags "my-new-tag"

    Then The monitored state will be "MONITORED"
