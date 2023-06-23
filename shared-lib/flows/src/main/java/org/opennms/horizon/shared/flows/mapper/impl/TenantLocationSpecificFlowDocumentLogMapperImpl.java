/**
 * This file is part of OpenNMS(R).
 * <p>
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p>
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 * http://www.gnu.org/licenses/
 * <p>
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 **/
package org.opennms.horizon.shared.flows.mapper.impl;

import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentLogMapper;

public class TenantLocationSpecificFlowDocumentLogMapperImpl implements TenantLocationSpecificFlowDocumentLogMapper {
    @Override
    public TenantLocationSpecificFlowDocumentLog mapBareToTenanted(String tenantId, String location, FlowDocumentLog flowDocumentLog) {
        return TenantLocationSpecificFlowDocumentLog.newBuilder()
            .setTenantId(tenantId)
            .setLocationId(location)
            .setSystemId(flowDocumentLog.getSystemId())
            .addAllMessage(flowDocumentLog.getMessageList())
            .build();
    }

    @Override
    public FlowDocumentLog mapTenantedToBare(TenantLocationSpecificFlowDocumentLog tenantLocationSpecificFlowDocumentLog) {
        return FlowDocumentLog.newBuilder()
            .setSystemId(tenantLocationSpecificFlowDocumentLog.getSystemId())
            .addAllMessage(tenantLocationSpecificFlowDocumentLog.getMessageList())
            .build();
    }
}
