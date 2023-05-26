# This file is part of OpenNMS(R).
#
# Copyright (C) 2023 The OpenNMS Group, Inc.
# OpenNMS(R) is Copyright (C) 1999-$today.year The OpenNMS Group, Inc.
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

Feature: Inventory Metrics

  Scenario: Collect tenant metrics from the Prometheus scrape endpoint
    Given Grpc TenantId "prometheus-scrape-test-tenant1"
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Application external http endpoint base url in system property "application-external-http-base-url"
    Given Expected HTTP response line matching regex "node_count\{tenant=\"prometheus-scrape-test-tenant1\",\} 1.0"
    Then Create Grpc Connection for Inventory
    Given [Common] Create "MINION-1" Location
    Then add a new device with label "local1" and ip address "127.0.0.1" and location "MINION-1"
    Then Send GET request to application at path "/actuator/prometheus", with timeout 5000ms, until successful response matches

  Scenario: Collect tenant metrics from the Prometheus scrape endpoint with two devices
    Given Grpc TenantId "prometheus-scrape-test-tenant2"
    Given External GRPC Port in system property "application-external-grpc-port"
    Given Application external http endpoint base url in system property "application-external-http-base-url"
    Given Expected HTTP response line matching regex "node_count\{tenant=\"prometheus-scrape-test-tenant2\",\} 2.0"
    Then Create Grpc Connection for Inventory
    Given [Common] Create "MINION-1" Location
    Then add a new device with label "local1" and ip address "127.0.0.1" and location "MINION-1"
    Then add a new device with label "local2" and ip address "127.0.0.2" and location "MINION-1"
    Then Send GET request to application at path "/actuator/prometheus", with timeout 5000ms, until successful response matches

