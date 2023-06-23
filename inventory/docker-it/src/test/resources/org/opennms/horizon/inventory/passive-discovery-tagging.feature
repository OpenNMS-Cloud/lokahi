Feature: Passive Discovery Tagging

  Background: Common Test Setup
    Given [PassiveDiscovery] External GRPC Port in system property "application-external-grpc-port"
    Given [PassiveDiscovery] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [PassiveDiscovery] Grpc TenantId "tenant-stream"
    Given [PassiveDiscovery] Create Grpc Connection for Inventory

  Scenario: Create new tags on passive discovery
    Given [Common] Create "Cork" Location
    Given Passive discovery communities "public"
    Given Passive discovery ports "161"
    Given Passive discovery location named "Cork"
    When A new passive discovery named "cork-public" is created
    When A GRPC request to create tags "tag1,tag2" for passive discovery with label "cork-public"
    Then Fetch tags for passive discovery "cork-public"
    Then The passive discovery tag response should contain only tags "tag1,tag2"

  Scenario: Create multiple tags on multiple passive discovery
    Given [Common] Create "Newcastle" Location
    Given [Common] Create "Dublin" Location
    Given Passive discovery communities "private"
    Given Passive discovery ports "161"
    Given Passive discovery location named "Newcastle"
    When A new passive discovery named "newcastle-private" is created
    Given Passive discovery communities "private"
    Given Passive discovery ports "161"
    Given Passive discovery location named "Dublin"
    Given A new passive discovery named "dublin-private" is created
    When A GRPC request to create tags "private1,private2" for passive discovery with label "newcastle-private"
    When A GRPC request to create tags "private1,private2" for passive discovery with label "dublin-private"
    Then Fetch tags for passive discovery "newcastle-private"
    Then The passive discovery tag response should contain only tags "private1,private2"
    Then Fetch tags for passive discovery "dublin-private"
    Then The passive discovery tag response should contain only tags "private1,private2"

  Scenario: Get a list of tags for passive discovery
    Given [Common] Create "Derry" Location
    Given Passive discovery communities "public"
    Given Passive discovery ports "161"
    Given Passive discovery location named "Derry"
    Given Passive discovery tags "d1,d2"
    Given A new passive discovery named "derry-public" is created
    When Fetch tags for passive discovery "derry-public"
    Then The passive discovery tag response should contain only tags "d1,d2"

  Scenario: Get an empty list of tags for passive discovery
    Given [Common] Create "Coleraine" Location
    Given Passive discovery communities "user"
    Given Passive discovery ports "161"
    Given Passive discovery location named "Coleraine"
    Given A new passive discovery named "coleraine-user" is created
    When A GRPC request to fetch tags for passive discovery with label "coleraine-user"
    Then The passive discovery tag response should contain an empty list of tags

  Scenario: Get a list of tags for passive discovery and name like provided search term
    Given [Common] Create "Cookstown" Location
    Given Passive discovery communities "private"
    Given Passive discovery ports "161"
    Given Passive discovery location named "Cookstown"
    Given Passive discovery tags "abc,bcd"
    Given A new passive discovery named "cookstown-private" is created
    When A GRPC request to fetch passive discovery "cookstown-private" tags with name like "cd"
    Then The passive discovery tag response should contain only tags "bcd"

  Scenario: Get an empty list of tags for passive discovery and name like provided search term
    Given [Common] Create "Armagh" Location
    Given Passive discovery communities "user"
    Given Passive discovery ports "161"
    Given Passive discovery location named "Armagh"
    Given Passive discovery tags "abc,bcd"
    Given A new passive discovery named "armagh-user" is created
    When A GRPC request to fetch passive discovery "armagh-user" tags with name like "xyz"
    Then The passive discovery tag response should contain an empty list of tags

  Scenario: Remove tags from passive discovery
    Given [Common] Create "Omagh" Location
    Given Passive discovery communities "community"
    Given Passive discovery ports "160,162"
    Given Passive discovery location named "Omagh"
    Given Passive discovery tags "tagMe1,tagMe2"
    Given A new passive discovery named "omagh-community" is created
    When A GRPC request to remove tag "tagMe1" for passive discovery with label "omagh-community"
    When A GRPC request to fetch tags for passive discovery with label "omagh-community"
    Then The passive discovery tag response should contain only tags "tagMe2"
