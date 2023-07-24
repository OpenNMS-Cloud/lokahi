@welcome
Feature: User can use welcome wizard to connect local minion to the OpenNMS and discover first node

  Scenario: Verify we can download certificate, start minion and discover first node in the welcome wizard
    Given Open the welcome wizard
    Given check 'Start Setup' button is accessible and visible
    Then click on 'Start Setup' button to start welcome wizard
    Then click on 'Download' button to get certificate and password for minion "minion_test_id_1" and start minion using "docker-compose-minion.yaml"
    Then wizard shows that minion connected successfully
    Then click on 'Continue' button
    Given Start snmp node "automated-testing-sysName"
    Then enter IP of "automated-testing-sysName" for discovery
    Then click on 'Start Discovery' button
    Then first node with system name "automated-testing-sysName" discovered successfully
    Then click on 'Continue' button to end the wizard
    Then Navigate to the "locations" through the left panel
    Then check "default" location exists
    Then click on location "default"
    Then check minion "minion_test_id_1" exists
    Then delete minion "minion_test_id_1"
    Then verify minion "minion_test_id_1" deleted
    Then delete node "automated-testing-sysName"






