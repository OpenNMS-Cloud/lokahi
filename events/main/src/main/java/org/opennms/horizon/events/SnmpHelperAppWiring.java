package org.opennms.horizon.events;

import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpHelperImpl;
import org.opennms.horizon.shared.snmp.SnmpStrategy;
import org.opennms.horizon.shared.snmp.snmp4j.Snmp4JStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnmpHelperAppWiring {
    @Bean
    public SnmpHelper snmpHelper(@Autowired SnmpStrategy snmpStrategy) {
        return new SnmpHelperImpl(snmpStrategy);
    }

    @Bean
    public Snmp4JStrategy snmp4JStrategy() {
        return new Snmp4JStrategy();
    }
}
