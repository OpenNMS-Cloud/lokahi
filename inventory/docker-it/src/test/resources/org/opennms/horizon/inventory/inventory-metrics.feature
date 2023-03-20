Feature: Inventory Metrics

  Scenario: Collect tenant metrics from the Prometheus scrape endpoint
    Given Grpc TenantId "prometheus-scrape-test-tenant1"
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Application external http endpoint base url in system property "application-external-http-base-url"
    Given Expected HTTP response line matching regex "node_count\{tenant=\"prometheus-scrape-test-tenant1\",\} 1.0"
    Then Create Grpc Connection for Inventory
    Then add a new device with label "local1" and ip address "127.0.0.1" and location "MINION-1"
    Then Send GET request to application at path "/actuator/prometheus", with timeout 5000ms, until successful response matches

  Scenario: Collect tenant metrics from the Prometheus scrape endpoint
    Given Grpc TenantId "prometheus-scrape-test-tenant2"
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Application external http endpoint base url in system property "application-external-http-base-url"
    Given Expected HTTP response line matching regex "node_count\{tenant=\"prometheus-scrape-test-tenant2\",\} 2.0"
    Then Create Grpc Connection for Inventory
    Then add a new device with label "local1" and ip address "127.0.0.1" and location "MINION-1"
    Then add a new device with label "local2" and ip address "127.0.0.2" and location "MINION-1"
    Then Send GET request to application at path "/actuator/prometheus", with timeout 5000ms, until successful response matches

