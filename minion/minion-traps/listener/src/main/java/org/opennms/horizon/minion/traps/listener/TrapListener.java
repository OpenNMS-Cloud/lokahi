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

import com.google.protobuf.ByteString;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapIdentity;
import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.logging.Logging;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpV3User;
import org.opennms.horizon.shared.snmp.SnmpVarBindDTO;
import org.opennms.horizon.shared.snmp.snmp4j.Snmp4JStrategy;
import org.opennms.horizon.shared.snmp.snmp4j.Snmp4JTrapNotifier;
import org.opennms.horizon.shared.snmp.snmp4j.Snmp4JUtils;
import org.opennms.horizon.shared.snmp.traps.TrapInformation;
import org.opennms.horizon.shared.snmp.traps.TrapNotificationListener;
import org.opennms.horizon.shared.snmp.traps.TrapdInstrumentation;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpResult;
import org.opennms.horizon.snmp.api.SnmpValue;
import org.opennms.sink.traps.contract.TrapConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrapListener implements TrapNotificationListener, Listener {

    private static final Logger LOG = LoggerFactory.getLogger(TrapListener.class);


    private final MessageDispatcherFactory messageDispatcherFactory;

    private final IpcIdentity identity;

    private AsyncDispatcher<TrapDTO> dispatcher;

    private final AtomicBoolean registeredForTraps = new AtomicBoolean(false);

    private final TrapConfig trapsBaseConfig;

    private final SnmpHelper snmpHelper;

    public static final TrapdInstrumentation trapdInstrumentation = new TrapdInstrumentation();

    public TrapListener(TrapConfig trapsBaseConfig,
                        MessageDispatcherFactory messageDispatcherFactory,
                        IpcIdentity identity,
                        SnmpHelper snmpHelper) {
        this.trapsBaseConfig = trapsBaseConfig;
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.identity = identity;
        this.snmpHelper = snmpHelper;
    }

    @Override
    public void trapReceived(TrapInformation trapInformation) {

        try {
            TrapDTO trapDTO = transformTrapInfo(trapInformation);
	    LOG.info("Received Trap {}", trapDTO);
            
            try {
                getMessageDispatcher().send(trapDTO);
            } catch (final Exception ex) {
                LOG.error("An error occured while forwarding trap {} for further processing. The trap will be dropped.", trapInformation, ex);
                // This trap will never reach the sink consumer
                trapdInstrumentation.incErrorCount();
            }
        } catch (Exception ex) {
            LOG.error("Received trap {} is not valid and cannot be processed. The trap will be dropped.", trapInformation, ex);
            // This trap will never reach the sink consumer
            trapdInstrumentation.incErrorCount();
        }
    }

    @Override
    public void trapError(int error, String msg) {
        LOG.warn("Error Processing Received Trap: error = {} {}", error, (msg != null ? ", ref = " + msg : ""));
    }

    private void open() {
        final int snmpTrapPort = trapsBaseConfig.getSnmpTrapPort();
        final InetAddress address = getInetAddress(trapsBaseConfig);
        try {
            LOG.info("Listening on {}:{}", address == null ? "[all interfaces]" : InetAddressUtils.str(address), snmpTrapPort);
            snmpHelper.registerForTraps(this, address, snmpTrapPort, transformFromProto(trapsBaseConfig.getSnmpV3UserList()));
            registeredForTraps.set(true);

            LOG.debug("init: Creating the trap session");
        } catch (final IOException e) {
            if (e instanceof java.net.BindException) {
                Logging.withPrefix("OpenNMS.Manager", new Runnable() {
                    @Override
                    public void run() {
                        LOG.error("init: Failed to listen on SNMP trap port {}, perhaps something else is already listening?", snmpTrapPort, e);
                    }
                });
                LOG.error("init: Failed to listen on SNMP trap port {}, perhaps something else is already listening?", snmpTrapPort, e);
                throw new UndeclaredThrowableException(e, "Failed to listen on SNMP trap port " + snmpTrapPort + ", perhaps something else is already listening?");
            } else {
                LOG.error("init: Failed to initialize SNMP trap socket on port {}", snmpTrapPort, e);
                throw new UndeclaredThrowableException(e, "Failed to initialize SNMP trap socket on port " + snmpTrapPort);
            }
        }
    }

    private List<SnmpV3User> transformFromProto(List<org.opennms.sink.traps.contract.SnmpV3User> snmpV3Users) {
        var snmpUsers = new ArrayList<SnmpV3User>();
        snmpV3Users.forEach((snmpV3User -> {
            SnmpV3User v3User = new SnmpV3User();
            v3User.setEngineId(snmpV3User.getEngineId());
            v3User.setAuthProtocol(snmpV3User.getAuthProtocol());
            v3User.setAuthPassPhrase(snmpV3User.getAuthPassphrase());
            v3User.setPrivProtocol(snmpV3User.getPrivacyProtocol());
            v3User.setPrivPassPhrase(snmpV3User.getPrivacyPassphrase());
            v3User.setSecurityLevel(snmpV3User.getSecurityLevel());
            v3User.setSecurityName(snmpV3User.getSecurityName());
            snmpUsers.add(v3User);
        }));
        return snmpUsers;
    }

    private void close() {
        try {
            if (registeredForTraps.get()) {
                LOG.debug("stop: Closing SNMP trap session.");
                snmpHelper.unregisterForTraps(this);
                registeredForTraps.set(false);
                LOG.info("stop: SNMP trap session closed.");
            } else {
                LOG.debug("stop: not attemping to closing SNMP trap session--it was never opened or already closed.");
            }
        } catch (final IOException e) {
            LOG.warn("stop: exception occurred closing session", e);
        } catch (final IllegalStateException e) {
            LOG.debug("stop: The SNMP session was already closed", e);
        }
    }


    private InetAddress getInetAddress(TrapConfig trapsBaseConfig) {
        if (trapsBaseConfig.getSnmpTrapAddress().equals("*")) {
            return null;
        }
        return InetAddressUtils.addr(trapsBaseConfig.getSnmpTrapAddress());
    }

    public void start() {
        this.open();
    }

    public void stop() {
        this.close();
        try {
            getMessageDispatcher().close();
        } catch (Exception e) {
            LOG.error("Exception while closing dispatcher ", e);
        }
    }


    private AsyncDispatcher<TrapDTO> getMessageDispatcher() {
        if (dispatcher == null) {
            dispatcher = messageDispatcherFactory.createAsyncDispatcher(new TrapSinkModule(trapsBaseConfig, identity));
        }
        return dispatcher;
    }

    private TrapDTO transformTrapInfo(TrapInformation trapInfo) {

        // Map variable bindings
        final List<SnmpResult> results = new ArrayList<>();
        for (int i = 0; i < trapInfo.getPduLength(); i++) {
            final SnmpVarBindDTO varBindDTO = trapInfo.getSnmpVarBindDTO(i);
            if (varBindDTO != null) {

                String oidStr = varBindDTO.getSnmpObjectId().toString();
                if (oidStr.length() > 0 && oidStr.charAt(0) != '.') {
                    // Always prepend a '.' to the string representation
                    // These won't get added automatically if the SnmpObjId is actually a SnmpInstId
                    oidStr = "." + oidStr;
                }
                SnmpResult snmpResult = SnmpResult.newBuilder()
                    .setBase(oidStr)
                    .setValue(SnmpValue.newBuilder()
                        .setTypeValue(varBindDTO.getSnmpValue().getType())
                        .setValue(ByteString.copyFrom(varBindDTO.getSnmpValue().getBytes())).build())
                    .build();
                results.add(snmpResult);
            }
        }

        TrapDTO.Builder trapDTOBuilder = TrapDTO.newBuilder()
            .setTrapAddress(InetAddressUtils.str(trapInfo.getAgentAddress()))
            .setAgentAddress(InetAddressUtils.str(trapInfo.getAgentAddress()))
            .setCommunity(trapInfo.getCommunity())
            .setVersion(trapInfo.getVersion())
            .setTimestamp(trapInfo.getTimeStamp())
            .setPduLength(trapInfo.getPduLength())
            .setCreationTime(trapInfo.getCreationTime())
            .setTrapIdentity(TrapIdentity.newBuilder()
                .setEnterpriseId(trapInfo.getTrapIdentity().getEnterpriseId())
                .setGeneric(trapInfo.getTrapIdentity().getGeneric())
                .setSpecific(trapInfo.getTrapIdentity().getSpecific())
                .setTrapOID(trapInfo.getTrapIdentity().getTrapOID())
                .build())
            .addAllSnmpResults(results);

        // include the raw message, if configured
        if (trapsBaseConfig.getIncludeRawMessage()) {
            byte[] rawMessage = convertToRawMessage(trapInfo);
            if (rawMessage != null) {
                trapDTOBuilder.setRawMessage(ByteString.copyFrom(rawMessage));
            }
        }
        return trapDTOBuilder.build();
    }


    /**
     * Converts the {@link TrapInformation} to a raw message.
     * This is only supported for Snmp4J {@link TrapInformation} implementations.
     *
     * @param trapInfo The Snmp4J {@link TrapInformation}
     * @return The bytes representing the raw message, or null if not supported
     */
    private static byte[] convertToRawMessage(TrapInformation trapInfo) {
        // Raw message conversion is not implemented for JoeSnmp, as the usage of that strategy is deprecated
        if (!(trapInfo instanceof Snmp4JTrapNotifier.Snmp4JV1TrapInformation)
            && !(trapInfo instanceof Snmp4JTrapNotifier.Snmp4JV2V3TrapInformation)) {
            LOG.warn("Unable to convert TrapInformation of type {} to raw message. " +
                    "Please use {} as snmp strategy to include raw messages",
                trapInfo.getClass(), Snmp4JStrategy.class);
            return null;
        }

        // Extract PDU
        try {
            PDU pdu = extractPDU(trapInfo);
            if (pdu != null) {
                return Snmp4JUtils.convertPduToBytes(trapInfo.getTrapAddress(), 0, trapInfo.getCommunity(), pdu);
            }
        } catch (Throwable e) {
            LOG.warn("Unable to convert PDU into bytes: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Retreive PDU from SNMP4j {@link TrapInformation}.
     */
    private static PDU extractPDU(TrapInformation trapInfo) {
        if (trapInfo instanceof Snmp4JTrapNotifier.Snmp4JV1TrapInformation) {
            return ((Snmp4JTrapNotifier.Snmp4JV1TrapInformation) trapInfo).getPdu();
        }
        if (trapInfo instanceof Snmp4JTrapNotifier.Snmp4JV2V3TrapInformation) {
            return ((Snmp4JTrapNotifier.Snmp4JV2V3TrapInformation) trapInfo).getPdu();
        }
        throw new IllegalArgumentException("Cannot extract PDU from trapInfo of type " + trapInfo.getClass());
    }
}
