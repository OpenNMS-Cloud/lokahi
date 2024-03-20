package org.opennms.horizon.shared.snmp.syslog;

import org.opennms.horizon.shared.snmp.SnmpV3User;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class SyslogListenerConfig {

    public static final String TWIN_KEY = "syslog.listener.config";

    private List<SnmpV3User> snmpV3Users = new ArrayList<>();

    public List<SnmpV3User> getSnmpV3Users() {
        return this.snmpV3Users;
    }

    public void setSnmpV3Users(final List<SnmpV3User> snmpV3Users) {
        this.snmpV3Users = snmpV3Users;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyslogListenerConfig)) {
            return false;
        }
        final SyslogListenerConfig that = (SyslogListenerConfig) o;
        return Objects.equals(this.snmpV3Users, that.snmpV3Users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.snmpV3Users);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SyslogListenerConfig.class.getSimpleName() + "[", "]")
            .add("snmpV3Users=" + snmpV3Users)
            .toString();
    }
}
