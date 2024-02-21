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

    public TrapListenerFactory(
            MessageDispatcherFactory messageDispatcherFactory, IpcIdentity identity, SnmpHelper snmpHelper) {
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
