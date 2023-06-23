Feature: Minion Certificate Manager RPC Request Processing

  Background: Common Test Setup
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Grpc TenantId "x-tenant-x"
    Given Create Grpc Connection

  Scenario: Send a get minion certificate request
    Given New Get Minion Certificate with tenantId "x-tenant-x" for location id 1010
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
    Then send isValid with last serial number and timeout 10000ms

  Scenario: Send two get minion certificate request
    Given New Get Minion Certificate with tenantId "x-tenant-x" for location id 2020
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
    Then send isValid with last serial number and timeout 10000ms

  Scenario: Send two get minion certificate request with different tenant id
    Given New Get Minion Certificate with tenantId "x-tenant-x" for location id 3030
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
    Then send isValid with last serial number and timeout 10000ms
    Given New Get Minion Certificate with tenantId "y-tenant-y" for location id 3031
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
    Then send isValid with last serial number and timeout 10000ms
