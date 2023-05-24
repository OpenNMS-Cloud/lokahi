Feature: Minion Gateway RPC Request Processing

  Background: Common Test Setup
    Given Internal GRPC Port in system property "application-internal-grpc-port"
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Kafka Bootstrap URL in system property "kafka.bootstrap-servers"

  Scenario: Send an RPC Request to a Minion that is not connected to the gateway
    Given mock tenant ID "x-tenant-x"
    Given mock location "x-location-001-x"
    Given Generated RPC Request ID
    Given RPC Request Tenant ID "x-tenant-x"
    Given RPC Request Location "x-location-001-x"
    Given RPC Request System ID "x-system-id-001-x"
    Given RPC Request Module ID "x-rpc-module-x"
    Given RPC Request TTL 3000ms
    Then send RPC Request with timeout 3000ms
    Then verify RPC exception was received
    Then verify RPC exception states active connection could not be found

  Scenario: Send an RPC Request to a "Minion" that is connected to the gateway
    Given mock tenant ID "x-tenant-x"
    Given mock system id "x-system-id-001-x"
    Given mock location "x-location-001-x"
    Then create Cloud RPC connection

    Given Generated RPC Request ID
    Given RPC Request Tenant ID "x-tenant-x"
    Given RPC Request Location "x-location-001-x"
    Given RPC Request System ID "x-system-id-001-x"
    Given RPC Request Module ID "x-rpc-module-x"
    Given RPC Request TTL 3000ms
    Then send RPC Request until successful with timeout 5000ms
    Then verify RPC request was received by test rpc request server with timeout 3000ms

  Scenario: Send a Task Set to a "Minion" that is connected to the gateway
    Given mock tenant ID "x-tenant-x"
    Given mock system id "x-system-id-001-x"
    Given mock location "x-location-001-x"
    Then create Cloud RPC connection
    Then create Cloud-To-Minion Message connection

    Then send task set to the Minion Gateway until successful with timeout 5000ms
    Then verify task set was received by cloud-to-minion message connection with timeout 3000ms

  Scenario: Send a Task Set Result to the Gateway and verify it is delivered to Kafka
    Given mock tenant ID "x-tenant-x"
    Given mock location "x-location-001-x"
    Then create Minion-to-Cloud Message connection
    Then send task set monitor result to the Minion Gateway until successful with timeout 5000ms
    Then verify task set result was published to Kafka with tenant id = "x-tenant-x" and timeout 3000ms

  Scenario: Register for Twin updates to the Gateway via the external GRPC API (MinionToCloudRPC)
    Given mock tenant ID "x-tenant-x"
    Given mock location "x-location-001-x"
    Then create Minion-to-Cloud RPC Request connection

    Given Generated RPC Request ID
    Given RPC Request System ID "x-system-id-001-x"
    Given RPC Request TTL 3000ms
    Then send twin update request to the Minion Gateway until successful with timeout 5000ms
    Then verify twin update response was received from the Minion Gateway

  Scenario: Verify EXCEPTION on invalid Minion Identity
    Given mock tenant ID "x-tenant-x"
    Given mock location "x-location-001-x"
    Given Generated RPC Request ID
    Given RPC Request Tenant ID "x-nosuch-tenant-x"
    Given RPC Request Location "x-nosuch-location-001-x"
    Given RPC Request System ID "x-nosuch-system-id-001-x"
    Given RPC Request Module ID "x-rpc-module-x"
    Given RPC Request TTL 3000ms
    Then send RPC Request with timeout 3000ms
    Then verify RPC exception was received
    Then verify RPC exception indicates missing connection for minion
