Feature: Minion Certificate Verifier HTTP Request Processing

  Background: Common Test Setup
    Given External HTTP port in system property "application-external-http-port"

  Scenario: Verify minion certificate validation request
    When Request with "OU=T:tenant01,OU=L:111,CN=opennms-minion-ssl-gateway,O=OpenNMS,L=TBD,ST=TBD,C=CA" is made
    Then Within 30s result headers are:
      | header             | value       |
      | tenant-id          | tenant01    |
      | location-id        | 111         |

  Scenario: Verify minion certificate validation request - with only tenant and location
    When Request with "OU=T:tenant02,OU=L:222" is made
    Then Within 30s result headers are:
      | header             | value       |
      | tenant-id          | tenant02    |
      | location-id        | 222         |

  Scenario: Verify minion certificate validation request - with only tenant
    When Request with "OU=T:tenant03" is made
    Then Within 30s result fails

  Scenario: Verify minion certificate validation request - with empty tenant
    When Request with "OU=T:,OU=L:333" is made
    Then Within 30s result fails


  Scenario: Verify minion certificate validation request - with only location
    When Request with "OU=L:ASD" is made
    Then Within 30s result fails

  Scenario: Verify minion certificate validation request - with empty location
    When Request with "OU=T:tenant03,OU=L:" is made
    Then Within 30s result fails

  Scenario: Verify minion certificate validation request - with empty subject
    When Request with "" is made
    Then Within 30s result fails

  Scenario: Verify minion certificate validation request - with expired certificate
    When Request with "OU=T:tenant03,OU=L:333" is made
    Then Within 30s result fails
