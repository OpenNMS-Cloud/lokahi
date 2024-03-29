/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.repository;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.taskset.contract.ScanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IpInterfaceRepository extends JpaRepository<IpInterface, Long> {
    List<IpInterface> findByTenantId(String tenantId);

    @Query("SELECT ip " + "FROM IpInterface ip "
            + "WHERE ip.ipAddress = :ipAddress "
            + "AND ip.node.monitoringLocationId = :locationId "
            + "AND ip.tenantId = :tenantId "
            + "ORDER BY ip.id")
    List<IpInterface> findByIpAddressAndLocationIdAndTenantId(
            @Param("ipAddress") InetAddress ipAddress,
            @Param("locationId") Long locationId,
            @Param("tenantId") String tenantId);

    @Query("SELECT ip " + "FROM IpInterface ip "
            + "WHERE ip.ipAddress = :ipAddress "
            + "AND ip.node.monitoringLocationId = :locationId "
            + "AND ip.tenantId = :tenantId "
            + "AND ip.node.scanType = :scanType ")
    Optional<IpInterface> findByIpLocationIdTenantAndScanType(
            @Param("ipAddress") InetAddress ipAddress,
            @Param("locationId") Long locationId,
            @Param("tenantId") String tenantId,
            @Param("scanType") ScanType scanType);

    Optional<IpInterface> findByIdAndTenantId(long id, String tenantId);

    List<IpInterface> findByNodeId(long nodeId);

    Optional<IpInterface> findByNodeIdAndTenantIdAndIpAddress(long nodeId, String tenantId, InetAddress ipAddress);

    Optional<IpInterface> findByNodeIdAndSnmpPrimary(long nodeId, boolean snmpPrimary);

    @Query("SELECT ip FROM IpInterface ip " + "WHERE ip.tenantId = :tenantId "
            + "AND ip.node.id = :nodeId "
            + "AND (LOWER(ip.hostname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "OR CAST( ip.ipAddress as string ) LIKE CONCAT('%', :searchTerm, '%'))")
    List<IpInterface> findAllByTenantIdAndNodeIdAndSearchTerm(
            @Param("tenantId") String tenantId, @Param("nodeId") long nodeId, @Param("searchTerm") String searchTerm);
}
