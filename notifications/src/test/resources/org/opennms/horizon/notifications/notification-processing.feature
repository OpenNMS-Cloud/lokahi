Feature: Notification Processing

  Background: Common Test Setup
    Given clean setup
    Given grpc setup

  Scenario: Populate config via grpc
    Given Integration "test-tenant" key set to "abc" via grpc
    Then verify "test-tenant" key is "abc"
    Then verify "other-tenant" key is not set
    Then tear down grpc setup

  Scenario: Populate config twice via grpc
    Given Integration "test-tenant" key set to "abc" then "abcd" via grpc
    Then verify "test-tenant" key is "abcd"
    Then tear down grpc setup

  Scenario: Populate two different configs via grpc
    Given Integration "test-tenant" key set to "abc", then Integration "other-tenant" key set to "abcd" via grpc
    Then verify "test-tenant" key is "abc"
    Then verify "other-tenant" key is "abcd"
    Then tear down grpc setup

  Scenario: Populate config with tenantId
    Given Integration key set to "abc" without tenantId
    Then verify exception "CannotCreateTransactionException" thrown

  Scenario: Post notification
    Given Integration "test-tenant" key set to "abc" via grpc
    Given Alarm posted via service with tenant "test-tenant"
    Then verify pager duty rest method is called

  Scenario: Try to post notification with no config
    Given Alarm posted via service with no config with tenant "test-tenant"
    Then verify exception "NotificationConfigUninitializedException" thrown
