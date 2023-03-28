Feature: Notification Processing

  Background: Common Test Setup
    Given clean setup
    Given grpc setup

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

  Scenario: Post notification
    Given Integration "test-tenant" key set to "abc" via grpc
    Given Alert posted via service with tenant "test-tenant"
    Then verify pager duty rest method is called 1 times

  Scenario: Try to post notification with no config
    Given Alert posted via service with no config with tenant "test-tenant"
    Then verify exception "NotificationConfigUninitializedException" thrown with message "PagerDuty config not initialized. Row count=0"

  Scenario: Will retry on failure to post notification to PagerDuty
    Given Integration "test-tenant" key set to "abc" via grpc
    And first attempt to post to PagerDuty will fail but should retry
    And Alert posted via service with tenant "test-tenant"
    Then verify pager duty rest method is called 2 times
