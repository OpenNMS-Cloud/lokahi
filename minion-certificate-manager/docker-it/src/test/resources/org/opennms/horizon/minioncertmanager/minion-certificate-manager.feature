Feature: Minion Certificate Manager RPC Request Processing

  Background: Common Test Setup
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Grpc TenantId "x-tenant-x"
    Given Create Grpc Connection

  Scenario: Send a get minion certificate request
    Given New Get Minion Certificate with tenantId "x-tenant-x" for location "x-LOC-x"
    Then send Get Minion Certificate Request with timeout 10000ms and verify success

  Scenario: Send two get minion certificate request
    Given New Get Minion Certificate with tenantId "x-tenant-x" for location "x-LOC-x"
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
    Then send Get Minion Certificate Request with timeout 10000ms and verify success

  Scenario: Send two get minion certificate request with different tenant id
    Given New Get Minion Certificate with tenantId "x-tenant-x" for location "x-LOC-x"
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
    Given New Get Minion Certificate with tenantId "y-tenant-y" for location "y-LOC-y"
    Then send Get Minion Certificate Request with timeout 10000ms and verify success
