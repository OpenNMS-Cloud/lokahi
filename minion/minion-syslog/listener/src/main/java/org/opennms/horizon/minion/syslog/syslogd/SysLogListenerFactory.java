package org.opennms.horizon.minion.syslog.syslogd;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.horizon.minion.plugin.api.ListenerFactory;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.sink.traps.contract.SyslogConfig;


public class SysLogListenerFactory implements ListenerFactory {

    private final MessageDispatcherFactory messageDispatcherFactory;

    private final IpcIdentity identity;

    private final SnmpHelper snmpHelper;

    public SysLogListenerFactory(
        MessageDispatcherFactory messageDispatcherFactory, IpcIdentity identity, SnmpHelper snmpHelper) {
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.identity = identity;
        this.snmpHelper = snmpHelper;
    }

    @Override
    public Listener create(Any config) {
        if (!config.is(SyslogConfig.class)) {
            throw new IllegalArgumentException("configuration must be TrapsConfig; type-url=" + config.getTypeUrl());
        }

        try {
            SyslogConfig trapsBaseConfig = config.unpack(SyslogConfig.class);
            return new SysLogListener(trapsBaseConfig, messageDispatcherFactory, identity, snmpHelper);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Error while parsing config with type-url=" + config.getTypeUrl());
        }
    }
}
