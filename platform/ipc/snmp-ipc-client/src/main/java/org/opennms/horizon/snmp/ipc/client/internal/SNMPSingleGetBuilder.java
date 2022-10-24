/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.horizon.snmp.ipc.client.internal;

import java.util.Collections;
import java.util.List;

import org.opennms.horizon.grpc.snmp.contract.SnmpGetRequest;
import org.opennms.horizon.grpc.snmp.contract.SnmpGetRequest.Builder;
import org.opennms.horizon.grpc.snmp.contract.SnmpMultiResponse;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.SnmpValueFactory;

public class SNMPSingleGetBuilder extends AbstractSNMPRequestBuilder<SnmpValue> {

    public SNMPSingleGetBuilder(LocationAwareSnmpClientRpcImpl client, SnmpAgentConfig agent, SnmpValueFactory valueFactory, SnmpObjId oid) {
        super(client, agent, valueFactory, buildGetRequests(oid), Collections.emptyList());
    }

    private static List<SnmpGetRequest> buildGetRequests(SnmpObjId oid) {
        Builder getRequest = SnmpGetRequest.newBuilder();
        getRequest.addAllOids(Collections.singletonList(oid.toString()));
        return Collections.singletonList(getRequest.build());
    }

    @Override
    protected SnmpValue processResponse(SnmpMultiResponse response) {
        return response.getResponsesList().stream()
            .flatMap(res -> res.getResultsList().stream())
            .findFirst()
            .map(result -> mapValue(result.getValue()))
            .orElse(null);
    }
}
