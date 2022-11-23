/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.inventory.repository;

import com.vladmihalcea.hibernate.type.basic.Inet;
import org.opennms.horizon.inventory.model.IpInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IpInterfaceRepository extends JpaRepository<IpInterface, Long> {
    List<IpInterface> findByTenantId(String tenantId);

    @Query("select ip from IpInterface ip" +
        " inner join Node n on ip.nodeId = n.id" +
        " inner join MonitoringLocation ml on n.monitoringLocationId = ml.id" +
        " where ip.ipAddress = ?1 and ml.location = ?2 and ip.tenantId = ?3")
   Optional<IpInterface> findByIpAddressAndLocationAndTenantId(Inet ipAddress, String location, String tenantId);

    @Query("SELECT ip " +
        "FROM IpInterface ip " +
        "WHERE ip.ipAddress = :ipAddress " +
        "AND ip.node.monitoringLocation.location = :location ")
    Optional<IpInterface> findByIpAddressAndLocation(@Param("ipAddress") Inet ipAddress,
                                                     @Param("location") String location);

    List<IpInterface> findByNodeId(long nodeId);
}
