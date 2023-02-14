Feature: Node Tagging

  Background: Common Test Setup
    Given [Tags] External GRPC Port in system property "application-external-grpc-port"
    Given [Tags] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Tags] Grpc TenantId "tenant-stream"
    Given [Tags] Create Grpc Connection for Inventory

  Scenario: Create new tags on node
    Given A new node
    When A GRPC request to create tags "tag1,tag2" for node
    Then The response should contain only tags "tag1,tag2"

  Scenario: Get a list of tags for node
    Given A new node with tags "tag1,tag2"
    When A GRPC request to fetch tags for node
    Then The response should contain only tags "tag1,tag2"

  Scenario: Get an empty list of tags for node
    Given A new node with no tags
    When A GRPC request to fetch tags for node
    Then The response should contain an empty list of tags

  Scenario: Remove tags from node
    Given A new node with tags "tag1,tag2"
    When A GRPC request to remove tag "tag1" for node
    Then The response should contain only tags "tag2"

  Scenario: Get a list of tags
    Given A new node with tags "tag1,tag2"
    Given Another node with tags "tag2,tag3"
    When A GRPC request to fetch all tags
    Then The response should contain only tags "tag1,tag2,tag3"

  Scenario: Get an empty list of tags
    Given A new node with no tags
    When A GRPC request to fetch all tags
    Then The response should contain an empty list of tags
