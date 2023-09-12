#*******************************************************************************
# This file is part of OpenNMS(R).
#
# Copyright (C) 2023 The OpenNMS Group, Inc.
# OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
#
# OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
#
# OpenNMS(R) is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published
# by the Free Software Foundation, either version 3 of the License,
# or (at your option) any later version.
#
# OpenNMS(R) is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with OpenNMS(R).  If not, see:
#      http://www.gnu.org/licenses/
#
# For more information contact:
#     OpenNMS(R) Licensing <license@opennms.org>
#     http://www.opennms.org/
#     http://www.opennms.com/
#******************************************************************************

@welcome
Feature: User can use welcome wizard to connect local minion to the OpenNMS and discover first node

  Scenario: Verify we can download certificate, start minion and discover first node in the welcome wizard
    Given check 'Start Setup' button is accessible and visible
    Then click on 'Start Setup' button to start welcome wizard
    Then click on 'Download' button to get certificate and password for minion "minion_test_id_1"
    Then wizard shows that minion connected successfully
    Then click on 'Continue' button
    Given Start snmp node "automated-testing-sysName"
    Then enter IP of "automated-testing-sysName" for discovery
    Then click on 'Start Discovery' button
    Then first node with system name "automated-testing-sysName" discovered successfully
    Then click on 'Continue' button to end the wizard
    Then delete node "automated-testing-sysName"
    Then Navigate to the "locations" through the left panel
    Then check "default" location exists
    Then click on location "default"
    Then check minion "minion_test_id_1" exists
    Then stop minion for location "default"
