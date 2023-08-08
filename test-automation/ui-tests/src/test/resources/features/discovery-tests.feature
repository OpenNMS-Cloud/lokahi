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

@discovery
Feature: User can discover devices

  Scenario: Discover a single device by IP
    Given Start snmp node "node1"
    When Discovery "SingleDiscovery" for node "node1" is created to discover by IP
    Then Status of "node1" should be "UP"

  Scenario: Discover a single device by IP with non-default SNMP config
    Given Start snmp node "node2" with additional snmpd configuration
    Then Discover "NonDefaultSNMPDiscovery" for node "node2", port 2661, community "myCommunityString"
    Then Status of "node2" should be "UP"
    Then check snmp interfaces exist for node "node2"

  Scenario: Discover 3 devices through a subnet scan
    Given Start snmp node "nodeA" with additional snmpd configuration
    Given Start snmp node "nodeB" with additional snmpd configuration
    Given Start snmp node "nodeC" with additional snmpd configuration
    Then Subnet mask discovery "SubnetDiscovery" for nodes with mask 28, port 2661, community "myCommunityString"
    Then Status of "nodeA" should be "UP"
    Then check snmp interfaces exist for node "nodeA"
    Then Status of "nodeB" should be "UP"
    Then check snmp interfaces exist for node "nodeB"
    Then Status of "nodeC" should be "UP"
    Then check snmp interfaces exist for node "nodeC"

  Scenario: Discover 4 devices through IP ranges
    Given Start snmp node "nodeD1" with additional snmpd configuration
    Given Start snmp node "nodeD2" with additional snmpd configuration
    Given Start snmp node "nodeD3" with additional snmpd configuration
    Given Start snmp node "nodeD4" with additional snmpd configuration
    Then Subnet range discovery "IPRangeDiscovery" for nodes with port 2661, community "myCommunityString"
    Then Status of "nodeD1" should be "UP"
    Then check snmp interfaces exist for node "nodeD1"
    Then Status of "nodeD2" should be "UP"
    Then check snmp interfaces exist for node "nodeD2"
    Then Status of "nodeD3" should be "UP"
    Then check snmp interfaces exist for node "nodeD3"
    Then Status of "nodeD4" should be "UP"
    Then check snmp interfaces exist for node "nodeD4"

  Scenario: Discover 3 of 4 devices with an IP list
    Given Start snmp node "nodeL1" with additional snmpd configuration
    Given Start snmp node "nodeL2" with additional snmpd configuration
    Given Start snmp node "nodeL3" with additional snmpd configuration
    Given Start snmp node "nodeL4" with additional snmpd configuration
    Then IP list discovery "IPListDiscovery" for nodes "nodeL1, nodeL2, nodeL4" with port 2661, community "myCommunityString"
    Then Status of "nodeL1" should be "UP"
    Then check snmp interfaces exist for node "nodeL1"
    Then Status of "nodeL2" should be "UP"
    Then check snmp interfaces exist for node "nodeL2"
    Then Node "nodeL3" should not exist
    Then Status of "nodeL4" should be "UP"
    Then check snmp interfaces exist for node "nodeL4"


