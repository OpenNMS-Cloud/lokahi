Feature: Monitoring Policy

  Background:
    Given clean setup
    And kafka setup

  Scenario: Can add new monitoring policy
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | JerrySeinfeld | true | false | false |
    Then verify "JerrySeinfeld" has a monitoring policy with ID 1 and the following enabled
      | PagerDuty |


  Scenario: Can update existing monitoring policy
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | JerrySeinfeld | false | true  | false |
      | 1 | JerrySeinfeld | true  | false | true  |
    Then verify "JerrySeinfeld" has a monitoring policy with ID 1 and the following enabled
      | PagerDuty |
      | webhooks  |

  Scenario: Can add multiple monitoring policies for a tenant
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | JerrySeinfeld | false | false | false |
      | 2 | JerrySeinfeld | true  | true  | true  |
    Then verify "JerrySeinfeld" has a monitoring policy with ID 1 and the following enabled
      |  |
    And verify "JerrySeinfeld" has a monitoring policy with ID 2 and the following enabled
      | PagerDuty |
      | email     |
      | webhooks  |

  Scenario: Can add multiple monitoring policies for different tenants
    Given the following monitoring policies sent via Kafka
      #| id | tenant        | enablePagerDuty | enableEmail | enableWebhooks |
      | 1 | CosmoKramer   | false | false | true |
      | 2 | JerrySeinfeld | false | true  | true |
    Then verify "CosmoKramer" has a monitoring policy with ID 1 and the following enabled
      | webhooks |
    And verify "JerrySeinfeld" has a monitoring policy with ID 2 and the following enabled
      | email    |
      | webhooks |
