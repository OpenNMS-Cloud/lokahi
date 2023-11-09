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

import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {
    List<MonitoredService> findByTenantId(String tenantId);

    @Query("SELECT ms " +
        "FROM MonitoredService ms " +
        "WHERE ms.tenantId = :tenantId " +
        "AND ms.monitoredServiceType = :monitoredServiceType " +
        "AND ms.ipInterface = :ipInterface")
    Optional<MonitoredService> findByTenantIdTypeAndIpInterface(@Param("tenantId") String tenantId,
                                                                @Param("monitoredServiceType") MonitoredServiceType monitoredServiceType,
                                                                @Param("ipInterface") IpInterface ipInterface);

    Optional<MonitoredService> findByIdAndTenantId(long id, String tenantId);


    @Query("SELECT ms " +
        "FROM MonitoredService ms " +
        "WHERE ms.tenantId = :tenantId " +
        "AND ms.monitoredServiceType.serviceName = :serviceName " +
        "AND ms.ipInterfaceId = :ipInterfaceId")
    Optional<MonitoredService> findByServiceNameAndIpInterfaceId(@Param("tenantId") String tenantId,
                                                                 @Param("serviceName") String serviceName,
                                                                 @Param("ipInterfaceId") long ipInterfaceId);

}
