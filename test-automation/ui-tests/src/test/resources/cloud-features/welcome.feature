@cloud
Feature: User can see use welcome wizard to connect local minion to the OpenNMS

   Scenario: Verify we can download certificate, start minion and discover first node in the welcome wizard
    Given check 'Start Setup' button is accessible and visible
    Then click on 'Start Setup' button to start welcome wizard
    Then click on 'Download' button to get certificate and password for minion "minion_test_id_1" and start minion using "docker-compose-cloud.yaml"
    Then wizard shows that minion connected successfully
    Then click on 'Continue' button
    Then enter IP "172.31.10.10" for discovery
    Then click on 'Start Discovery' button
    Then first node with IP "172.31.10.10" discovered successfully
    Then click on 'Continue' button to end the wizard
    When Navigate to the "locations" through the left panel
    Then check "default" location exists
    Then click on location "default"
    Then check minion "minion_test_id_1" exists
    Then delete minion "minion_test_id_1"
    Then verify minion "minion_test_id_1" deleted





