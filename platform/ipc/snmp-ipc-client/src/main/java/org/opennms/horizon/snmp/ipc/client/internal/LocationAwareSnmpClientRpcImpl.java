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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.grpc.snmp.contract.SnmpMultiResponse;
import org.opennms.horizon.grpc.snmp.contract.SnmpRequest;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;
import org.opennms.horizon.shared.snmp.CollectionTracker;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpResult;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.SnmpValueFactory;
import org.opennms.horizon.shared.snmp.proxy.LocationAwareSnmpClient;
import org.opennms.horizon.shared.snmp.proxy.SNMPRequestBuilder;

/**
 * Location-aware SNMP client that builds a {@link SnmpRequest} and delegates
 * the request to either a local, or a remote @{link SnmpRequestExecutor}.
 *
 * @author jwhite
 */
public class LocationAwareSnmpClientRpcImpl implements LocationAwareSnmpClient {

    private final RpcClient<SnmpMultiResponse> client;
    private final SnmpValueFactory valueFactory;

    public LocationAwareSnmpClientRpcImpl(RpcClientFactory rpcClientFactory, SnmpValueFactory valueFactory) {
        this.client = rpcClientFactory.getClient(response -> response.getPayload().unpack(SnmpMultiResponse.class));
        this.valueFactory = valueFactory;
    }

    @Override
    public SNMPRequestBuilder<List<SnmpResult>> walk(SnmpAgentConfig agent, String... oids) {
        final List<SnmpObjId> snmpObjIds = Arrays.stream(oids)
            .map(SnmpObjId::get)
            .collect(Collectors.toList());
        return walk(agent, snmpObjIds);
    }

    @Override
    public SNMPRequestBuilder<List<SnmpResult>> walk(SnmpAgentConfig agent, SnmpObjId... oids) {
        return walk(agent, Arrays.asList(oids));
    }

    @Override
    public SNMPRequestBuilder<List<SnmpResult>> walk(SnmpAgentConfig agent, List<SnmpObjId> oids) {
        return new SNMPWalkBuilder(this, agent, valueFactory, oids);
    }

    @Override
    public SNMPRequestBuilder<CollectionTracker> walk(SnmpAgentConfig agent, CollectionTracker tracker) {
        return new SNMPWalkWithTrackerBuilder(this, agent, valueFactory, tracker);
    }

    @Override
    public SNMPRequestBuilder<SnmpValue> get(SnmpAgentConfig agent, String oid) {
        return get(agent, SnmpObjId.get(oid));
    }

    @Override
    public SNMPRequestBuilder<SnmpValue> get(SnmpAgentConfig agent, SnmpObjId oid) {
        return new SNMPSingleGetBuilder(this, agent, valueFactory, oid);
    }

    @Override
    public SNMPRequestBuilder<List<SnmpValue>> get(SnmpAgentConfig agent, String... oids) {
        final List<SnmpObjId> snmpObjIds = Arrays.stream(oids)
            .map(SnmpObjId::get)
            .collect(Collectors.toList());
        return get(agent, snmpObjIds);
    }

    @Override
    public SNMPRequestBuilder<List<SnmpValue>> get(SnmpAgentConfig agent, SnmpObjId... oids) {
        return get(agent, Arrays.asList(oids));
    }

    @Override
    public SNMPRequestBuilder<List<SnmpValue>> get(SnmpAgentConfig agent, List<SnmpObjId> oids) {
        return new SNMPMultiGetBuilder(this, agent, valueFactory, oids);
    }

    CompletableFuture<SnmpMultiResponse> execute(String location, String systemId, SnmpRequest payload) {
        RpcRequestProto request = client.builder("SNMP")
            .withLocation(location)
            .withSystemId(systemId)
            .withExpirationTime(0L)
            .withPayload(payload)
            .build();
        return client.execute(request);
    }
}
