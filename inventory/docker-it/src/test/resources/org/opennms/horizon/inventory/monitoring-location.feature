@monitoring-location
Feature: Monitoring Location

  Background:
    Given [MonitoringLocation] External GRPC Port in system property "application-external-grpc-port"
    Given [MonitoringLocation] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"

  Scenario: Happy Path
    Given [MonitoringLocation] Grpc TenantId "tenant-stream"
    Given [MonitoringLocation] Create Grpc Connection for Inventory
    When [MonitoringLocation] Clean up Monitoring Location
    Then [MonitoringLocation] Monitoring Location is cleaned up
    When [MonitoringLocation] Create Monitoring Location with name "xxx-LOC-xxx"
    Then [MonitoringLocation] Monitoring Location is created
    When [MonitoringLocation] Get Monitoring Location by name "xxx-LOC-xxx"
    Then [MonitoringLocation] Monitoring Location is returned
    When [MonitoringLocation] Get Monitoring Location by id
    Then [MonitoringLocation] Monitoring Location is returned
    When [MonitoringLocation] Update Monitoring Location with name "yyy-LOC-yyy"
    Then [MonitoringLocation] Monitoring Location is updated
    When [MonitoringLocation] Get Monitoring Location by name "yyy-LOC-yyy"
    Then [MonitoringLocation] Monitoring Location is returned
    When [MonitoringLocation] Delete Monitoring Location
    Then [MonitoringLocation] Monitoring Location is deleted
    Then [MonitoringLocation] Monitoring Location is not found

  Scenario: List Location With Wrong Tenant
    Given [MonitoringLocation] Grpc TenantId "wrong-tenant"
    Given [MonitoringLocation] Create Grpc Connection for Inventory
    Given [MonitoringLocation] Create Monitoring Location with name "xxx-LOC-xxx"
    Given [MonitoringLocation] Create Monitoring Location with name "yyy-LOC-yyy"
    Given [MonitoringLocation] Grpc TenantId "other-tenant"
    Given [MonitoringLocation] Create Grpc Connection for Inventory
    When [MonitoringLocation] List Monitoring Location
    Then [MonitoringLocation] Nothing is found

  Scenario: Find By Name Not Found
    Given [MonitoringLocation] Grpc TenantId "other-tenant"
    Given [MonitoringLocation] Create Grpc Connection for Inventory
    Given [MonitoringLocation] Create Monitoring Location with name "xxx-LOC-xxx"
    Then [MonitoringLocation] Get Monitoring Location by name "yyy-LOC-yyy" Not Found
