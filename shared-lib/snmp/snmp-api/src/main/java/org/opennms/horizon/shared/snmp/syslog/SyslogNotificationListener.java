package org.opennms.horizon.shared.snmp.syslog;

import org.opennms.horizon.shared.snmp.traps.TrapInformation;

public interface SyslogNotificationListener {
    void syslogReceived(SyslogInformation syslogInformation);

    void syslogError(int error, String msg);
}
