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

package org.opennms.horizon.server.model.inventory;

import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.server.model.inventory.tag.Tag;

import java.util.List;

@Getter
@Setter
public class Node {
    private long id;
    private String nodeLabel;
    private String scanType;
    private String monitoredState;
    private long createTime;
    private long monitoringLocationId;
    private List<Tag> tags;
    private List<IpInterface> ipInterfaces;
    private List<SnmpInterface> snmpInterfaces;
    private List<AzureInterface> azureInterfaces;
    private String objectId;
    private String systemName;
    private String systemDescr;
    private String systemLocation;
    private String systemContact;
}
