@TagNode
Feature: Node Tagging

  Background: Common Test Setup
    Given [Tags] External GRPC Port in system property "application-external-grpc-port"
    Given [Tags] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Tags] Grpc TenantId "tenant-stream"
    Given [Tags] Create Grpc Connection for Inventory
    Given Kafka topic "tag-operation"

  Scenario: Create new tags on node
    Given A new node
    When A GRPC request to create tags "tag1,tag2" for node
    Then The node tag response should contain only tags "tag1,tag2"
    Then Verify Kafka message with 1 node
      | action     | tenant_id     | tag_name |
      | ASSIGN_TAG | tenant-stream | tag1     |
      | ASSIGN_TAG | tenant-stream | tag2     |

  Scenario: Create multiple tags on multiple nodes
    Given 2 new nodes
    When A GRPC request to create tags "tag1,tag2" for both nodes
    Then The node tag response should contain only tags "tag1,tag2"
    And Both nodes have the same tags of "tag1,tag2"
    Then Verify Kafka message with 2 nodes
      | action     | tenant_id     | tag_name |
      | ASSIGN_TAG | tenant-stream | tag1     |
      | ASSIGN_TAG | tenant-stream | tag2     |

  Scenario: Get a list of tags for node
    Given A new node with tags "tag1,tag2"
    When A GRPC request to fetch tags for node
    Then The node tag response should contain only tags "tag1,tag2"

  Scenario: Get a list of nodes for tags
    Given 2 new nodes
    When A GRPC request to create tags "tag1,tag2" for both nodes
    Then The node tag response should contain only tags "tag1,tag2"
    Then A GRPC request to get nodes for tags "tag1,tag2"
    Then Both nodes should be fetched for

  Scenario: Get an empty list of tags for node
    Given A new node
    When A GRPC request to fetch tags for node
    Then The response should contain an empty list of tags

  Scenario: Get a list of tags for node and name like provided search term
    Given A new node with tags "abc,bcd"
    When A GRPC request to fetch all tags for node with name like "cd"
    Then The node tag response should contain only tags "bcd"

  Scenario: Get an empty list of tags for node and name like provided search term
    Given A new node with tags "abc,bcd"
    When A GRPC request to fetch all tags for node with name like "xyz"
    Then The response should contain an empty list of tags

  Scenario: Remove tags from node
    Given A new node with tags "tag1,tag2"
    When A GRPC request to remove tag "tag1" for node
    Then The node tag response should contain only tags "tag2"
    Then Verify Kafka message with 1 node
      | action     | tenant_id     | tag_name |
      | REMOVE_TAG | tenant-stream | tag1     |
    Then Delete the node
    Then Verify Kafka message with 1 node
      | action     | tenant_id     | tag_name |
      | REMOVE_TAG | tenant-stream | tag2     |


  Scenario: Get a list of tags
    Given A new node with tags "tag1,tag2"
    Given Another node with tags "tag2,tag3"
    When A GRPC request to fetch all tags
    Then The node tag response should contain only tags "tag1,tag2,tag3"

  Scenario: Get a list of tags with name like provided search term
    Given A new node with tags "abc,bcd"
    Given Another node with tags "cde,defg"
    When A GRPC request to fetch all tags with name like "cd"
    Then The node tag response should contain only tags "bcd,cde"

  Scenario: Get an empty list of tags with name like provided search term
    Given A new node with tags "abc,bcd"
    Given Another node with tags "cde,defg"
    When A GRPC request to fetch all tags with name like "xyz"
    Then The response should contain an empty list of tags
