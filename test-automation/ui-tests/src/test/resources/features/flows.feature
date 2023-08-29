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

@flows
Feature: NetFlows basic function

  @ignore
  Scenario: User sees empty 'Flows' chart before data was sent
    Given No netflow data was sent
    And sees "No applications data was found in last 24 hours" subtitle in the Top 10 Applications chart
    Then click on 'Flows' link
    And sees 'No Data' in the flows table

  @ignore
  Scenario: User sees flows data sent to the instance
    # Missing discoverable image needs to be merged into repo, then the rest
    # of these tests can be merged with the other test code using TestContainers

    Given Start snmp node "flowNode"
#    When Discovery "SingleDiscovery" for node "flowNode" is created to discover by IP
#    Then Status of "flowNode" should be "UP"

#    Then Navigate to the "" through the left panel
#    And wait until the 'Top 10 Applications' chart will reflect the received data
#    Then click on 'Flows' link
#    And sees chart for netflow data
#    Then click on 'Exporter' filter
#    And check if exporter "flowNode" visible in the dropdown

