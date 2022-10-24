/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.db.dao.api;

import org.opennms.horizon.db.model.OnmsMonitoredService;

import java.net.InetAddress;
import java.util.List;

/**
 * <p>MonitoredServiceDao interface.</p>
 *
 * @author Craig Gallen
 * @author David Hustace
 */
public interface MonitoredServiceDao extends OnmsDao<OnmsMonitoredService, Integer> {

    /**
     * <p>get</p>
     * 
     * @param nodeId a {@link Integer} object.
     * @param ipAddress a {@link InetAddress} object.
     * @param serviceId a {@link Integer} object.
     * @return a {@link OnmsMonitoredService} object.
     */
    OnmsMonitoredService get(Integer nodeId, InetAddress ipAddress, Integer serviceId);

    /**
     * <p>get</p>
     *
     * @param nodeId a {@link Integer} object.
     * @param ipAddr a {@link InetAddress} object.
     * @param ifIndex a {@link Integer} object.
     * @param serviceId a {@link Integer} object.
     * @return a {@link OnmsMonitoredService} object.
     */
    OnmsMonitoredService get(Integer nodeId, InetAddress ipAddr, Integer ifIndex, Integer serviceId);

    /**
     * <p>get</p>
     *
     * @param nodeId a {@link Integer} object.
     * @param ipAddress a {@link String} object.
     * @param svcName a {@link String} object.
     * @return a {@link OnmsMonitoredService} object.
     */
    OnmsMonitoredService get(Integer nodeId, InetAddress ipAddress, String svcName);

    /**
     * <p>findByType</p>
     *
     * @param typeName a {@link String} object.
     * @return a {@link java.util.Collection} object.
     */
    List<OnmsMonitoredService> findByType(String typeName);
}
