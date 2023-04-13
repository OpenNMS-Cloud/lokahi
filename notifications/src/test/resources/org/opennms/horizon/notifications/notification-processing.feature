Feature: Notification Processing

  Background: Common Test Setup
    Given clean setup
    Given grpc setup
    And kafka setup

  # Throws org.springframework.dao.DataIntegrityViolationException: assigned tenant id differs from current tenant id: test-tenant!=other-tenant : org.opennms.horizon.notifications.model.PagerDutyConfig.tenantId
  # which is then wrapped by the grpc
  Scenario: Populate config via grpc with different tenant
    Given Integration "test-tenant" key set to "abc" via grpc with token tenant "other-tenant"
    Then verify exception "StatusRuntimeException" thrown with message "UNKNOWN"

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

  Scenario: Populate config without tenantId
    Given Integration key set to "abc" without tenantId
    Then verify exception "JpaSystemException" thrown with message "SessionFactory configured for multi-tenancy, but no tenant identifier specified"

  Scenario: Post PagerDuty notification
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | test-tenant    | true            | false       | false          |
    And Integration "test-tenant" key set to "abc" via grpc
    And "test-tenant" has a monitoring policy with ID 1
    And Alert posted via service with tenant "test-tenant" with monitoring policy ID 1
    Then verify pager duty rest method is called 1 times

  Scenario: Will retry on failure to post notification to PagerDuty
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | test-tenant    | true            | false       | false          |
    And first attempt to post to PagerDuty will fail but should retry
    And Integration "test-tenant" key set to "abc" via grpc
    And "test-tenant" has a monitoring policy with ID 1
    And Alert posted via service with tenant "test-tenant" with monitoring policy ID 1
    Then verify pager duty rest method is called 2 times

  Scenario: Notification without monitoring policy is dropped
    Given Integration "test-tenant" key set to "abc" via grpc
    And Alert posted via service with tenant "test-tenant" with monitoring policy ID 1
    Then verify pager duty rest method is called 0 times

  Scenario: Notification not sent to PagerDuty if disabled in monitoring policy
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | test-tenant    | false            | false       | false          |
    And Integration "test-tenant" key set to "abc" via grpc
    And "test-tenant" has a monitoring policy with ID 1
    And Alert posted via service with tenant "test-tenant" with monitoring policy ID 1
    Then verify pager duty rest method is called 0 times

  Scenario: Send email notification
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | test-tenant    | false            | true        | false          |
    And "test-tenant" has email "admin@company.com"
    And "test-tenant" has a monitoring policy with ID 1
    And Alert posted via service with tenant "test-tenant" with monitoring policy ID 1
    Then verify alert is sent by email
