Feature: Tags

  Background: Common Test Setup
    Given [Tags] External GRPC Port in system property "application-external-grpc-port"
    Given [Tags] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Tags] Grpc TenantId "tenant-stream"
    Given [Tags] Create Grpc Connection for Inventory

  Scenario: Get a list of tags for node
    Given A new node with tags "tag1,tag2"
    When A GRPC request to fetch tags for node
    Then The response should contain only tags "tag1,tag2"

  Scenario: Get an empty list of tags for node
    Given A new node with no tags
    When A GRPC request to fetch tags for node
    Then The response should contain an empty list of tags

