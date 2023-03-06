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

package org.opennms.horizon.minion.traps.listener;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.horizon.minion.plugin.api.ListenerFactory;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.sink.traps.contract.TrapConfig;

public class TrapListenerFactory implements ListenerFactory {

    private final MessageDispatcherFactory messageDispatcherFactory;

    private final IpcIdentity identity;

    private final SnmpHelper snmpHelper;

    public TrapListenerFactory(MessageDispatcherFactory messageDispatcherFactory, IpcIdentity identity, SnmpHelper snmpHelper) {
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.identity = identity;
        this.snmpHelper = snmpHelper;
    }

    @Override
    public Listener create(Any config) {
        if (!config.is(TrapConfig.class)) {
            throw new IllegalArgumentException("configuration must be TrapsConfig; type-url=" + config.getTypeUrl());
        }

        try {
            TrapConfig trapsBaseConfig = config.unpack(TrapConfig.class);
            return new TrapListener(trapsBaseConfig, messageDispatcherFactory, identity, snmpHelper);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Error while parsing config with type-url=" + config.getTypeUrl());
        }

    }

}
