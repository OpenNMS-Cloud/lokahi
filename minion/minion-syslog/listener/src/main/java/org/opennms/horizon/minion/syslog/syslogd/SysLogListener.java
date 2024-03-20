package org.opennms.horizon.minion.syslog.syslogd;

import com.google.protobuf.ByteString;
import org.opennms.horizon.grpc.syslog.contract.SyslogDTO;
import org.opennms.horizon.grpc.syslog.contract.SyslogIdentity;
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
import org.opennms.horizon.shared.snmp.syslog.SyslogInformation;
import org.opennms.horizon.shared.snmp.syslog.SyslogNotificationListener;
import org.opennms.horizon.shared.snmp.syslog.SyslogdInstrumentation;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpResult;
import org.opennms.horizon.snmp.api.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.opennms.sink.traps.contract.SyslogConfig;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SysLogListener implements SyslogNotificationListener, Listener {

    private static final Logger LOG = LoggerFactory.getLogger(SysLogListener.class);

    private final MessageDispatcherFactory messageDispatcherFactory;

    private final IpcIdentity identity;

    private AsyncDispatcher<SyslogDTO> dispatcher;

    private final AtomicBoolean registeredForTraps = new AtomicBoolean(false);

    private final SyslogConfig syslogBaseConfig;

    private final SnmpHelper snmpHelper;

    public static final SyslogdInstrumentation trapdInstrumentation = new SyslogdInstrumentation();

    public SysLogListener(
        SyslogConfig syslogBaseConfig,
        MessageDispatcherFactory messageDispatcherFactory,
        IpcIdentity identity,
        SnmpHelper snmpHelper) {
        this.syslogBaseConfig = syslogBaseConfig;
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.identity = identity;
        this.snmpHelper = snmpHelper;
    }

    @Override
    public void syslogReceived(SyslogInformation trapInformation) {

        try {
            SyslogDTO syslogDTO = transformSyslogInfo(trapInformation);
            LOG.info("Received Trap {}", syslogDTO);

            try {
                getMessageDispatcher().send(syslogDTO);
            } catch (final Exception ex) {
                LOG.error(
                    "An error occured while forwarding syslog {} for further processing. The trap will be dropped.",
                    trapInformation,
                    ex);
                // This trap will never reach the sink consumer
                trapdInstrumentation.incErrorCount();
            }
        } catch (Exception ex) {
            LOG.error(
                "Received trap {} is not valid and cannot be processed. The trap will be dropped.",
                trapInformation,
                ex);
            // This trap will never reach the sink consumer
            trapdInstrumentation.incErrorCount();
        }
    }

    @Override
    public void syslogError(int error, String msg) {
        LOG.warn("Error Processing Received Syslog: error = {} {}", error, (msg != null ? ", ref = " + msg : ""));
    }

    private void open() {
        final int snmpTrapPort = syslogBaseConfig.getSyslogPort();
        final InetAddress address = getInetAddress(syslogBaseConfig);
        try {
            LOG.info(
                "Listening on {}:{}",
                address == null ? "[all interfaces]" : InetAddressUtils.str(address),
                snmpTrapPort);
            snmpHelper.registerForSyslog(
                this, address, snmpTrapPort, transformFromProto(syslogBaseConfig.getSnmpV3UserList()));
            registeredForTraps.set(true);

            LOG.debug("init: Creating the syslog session");
        } catch (final IOException e) {
            if (e instanceof java.net.BindException) {
                Logging.withPrefix("OpenNMS.Manager", new Runnable() {
                    @Override
                    public void run() {
                        LOG.error(
                            "init: Failed to listen on SNMP trap port {}, perhaps something else is already listening?",
                            snmpTrapPort,
                            e);
                    }
                });
                LOG.error(
                    "init: Failed to listen on SNMP trap port {}, perhaps something else is already listening?",
                    snmpTrapPort,
                    e);
                throw new UndeclaredThrowableException(
                    e,
                    "Failed to listen on SNMP trap port " + snmpTrapPort
                        + ", perhaps something else is already listening?");
            } else {
                LOG.error("init: Failed to initialize SNMP trap socket on port {}", snmpTrapPort, e);
                throw new UndeclaredThrowableException(
                    e, "Failed to initialize SNMP trap socket on port " + snmpTrapPort);
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
                snmpHelper.unregisterForSyslog(this);
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

    private InetAddress getInetAddress(SyslogConfig trapsBaseConfig) {
        if (trapsBaseConfig.getSyslogAddress().equals("*")) {
            return null;
        }
        return InetAddressUtils.addr(trapsBaseConfig.getSyslogAddress());
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

    private AsyncDispatcher<SyslogDTO> getMessageDispatcher() throws IOException {
        if (dispatcher == null) {
            dispatcher = messageDispatcherFactory.createAsyncDispatcher(new SysLogSinkModule(syslogBaseConfig, identity));
        }
        return dispatcher;
    }

    private SyslogDTO transformSyslogInfo(SyslogInformation syslogInfo) {

        // Map variable bindings
        final List<SnmpResult> results = new ArrayList<>();
        for (int i = 0; i < syslogInfo.getPduLength(); i++) {
            final SnmpVarBindDTO varBindDTO = syslogInfo.getSnmpVarBindDTO(i);
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
                        .setValue(ByteString.copyFrom(
                            varBindDTO.getSnmpValue().getBytes()))
                        .build())
                    .build();
                results.add(snmpResult);
            }
        }

        SyslogDTO.Builder syslogDTOBuilder = SyslogDTO.newBuilder()
            .setSyslogAddress(InetAddressUtils.str(syslogInfo.getAgentAddress()))
            .setAgentAddress(InetAddressUtils.str(syslogInfo.getAgentAddress()))
            .setCommunity(syslogInfo.getCommunity())
            .setVersion(syslogInfo.getVersion())
            .setTimestamp(syslogInfo.getTimeStamp())
            .setPduLength(syslogInfo.getPduLength())
            .setCreationTime(syslogInfo.getCreationTime())
            .setSyslogIdentity(SyslogIdentity.newBuilder()
                .setEnterpriseId(syslogInfo.getSyslogIdentity().getEnterpriseId())
                .setGeneric(syslogInfo.getSyslogIdentity().getGeneric())
                .setSpecific(syslogInfo.getSyslogIdentity().getSpecific())
                .setSyslogOID(syslogInfo.getSyslogIdentity().getSyslogOID())
                .build())
            .addAllSnmpResults(results);

        // include the raw message, if configured
        if (syslogBaseConfig.getIncludeRawMessage()) {
            byte[] rawMessage = convertToRawMessage(syslogInfo);
            if (rawMessage != null) {
                syslogDTOBuilder.setRawMessage(ByteString.copyFrom(rawMessage));
            }
        }
        return syslogDTOBuilder.build();
    }

    /**
     * Converts the {@link TrapInformation} to a raw message.
     * This is only supported for Snmp4J {@link TrapInformation} implementations.
     *
     * @param trapInfo The Snmp4J {@link TrapInformation}
     * @return The bytes representing the raw message, or null if not supported
     */
    private static byte[] convertToRawMessage(SyslogInformation trapInfo) {
        // Raw message conversion is not implemented for JoeSnmp, as the usage of that strategy is deprecated
        if (!(trapInfo instanceof Snmp4JTrapNotifier.Snmp4JV1SyslogInformation)
            && !(trapInfo instanceof Snmp4JTrapNotifier.Snmp4JV1SyslogInformation)) {
            LOG.warn(
                "Unable to convert TrapInformation of type {} to raw message. "
                    + "Please use {} as snmp strategy to include raw messages",
                trapInfo.getClass(),
                Snmp4JStrategy.class);
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
    private static PDU extractPDU(SyslogInformation trapInfo) {
        if (trapInfo instanceof Snmp4JTrapNotifier.Snmp4JV1SyslogInformation) {
            return ((Snmp4JTrapNotifier.Snmp4JV1SyslogInformation) trapInfo).getPdu();
        }

        throw new IllegalArgumentException("Cannot extract PDU from trapInfo of type " + trapInfo.getClass());
    }
}
