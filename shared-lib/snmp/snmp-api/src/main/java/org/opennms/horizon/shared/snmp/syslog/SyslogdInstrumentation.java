package org.opennms.horizon.shared.snmp.syslog;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class SyslogdInstrumentation {
    private static final Logger LOG = LoggerFactory.getLogger(SyslogdInstrumentation.class);

    private final AtomicLong syslogReceived = new AtomicLong();
    private final AtomicLong v1SyslogReceived = new AtomicLong();
    private final AtomicLong v2cSyslogReceived = new AtomicLong();
    private final AtomicLong v3SyslogReceived = new AtomicLong();
    private final AtomicLong vUnknownTrapsReceived = new AtomicLong();
    private final AtomicLong syslogDiscarded = new AtomicLong();
    private final AtomicLong syslogErrored = new AtomicLong();

    public void incSyslogReceivedCount(String version) {
        syslogReceived.incrementAndGet();
        if ("v1".equals(version)) {
            v1SyslogReceived.incrementAndGet();
        } else if ("v2c".equals(version) || "v2".equals(version)) {
            v2cSyslogReceived.incrementAndGet();
        } else if ("v3".equals(version)) {
            v3SyslogReceived.incrementAndGet();
        } else {
            vUnknownTrapsReceived.incrementAndGet();
            LOG.warn("Received a trap with an unknown SNMP protocol version '{}'.", version);
        }
    }

    public AtomicLong getSyslogReceived() {
        return syslogReceived;
    }

    public AtomicLong getV1SyslogReceived() {
        return v1SyslogReceived;
    }

    public AtomicLong getV2cSyslogReceived() {
        return v2cSyslogReceived;
    }

    public AtomicLong getV3SyslogReceived() {
        return v3SyslogReceived;
    }

    public AtomicLong getvUnknownTrapsReceived() {
        return vUnknownTrapsReceived;
    }

    public AtomicLong getSyslogDiscarded() {
        return syslogDiscarded;
    }

    public AtomicLong getSyslogErrored() {
        return syslogErrored;
    }

    public void incErrorCount() {
        syslogErrored.incrementAndGet();
    }

}
