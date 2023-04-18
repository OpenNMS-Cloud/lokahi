@cloud
Feature: User can see a minion connected to the instance

  Scenario: Verify that we are able to login to the cluster
    Given Navigate to the 'Appliances' through the left panel
    Then check minion in the list
