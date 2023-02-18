package org.opennms.horizon.minion.icmp;

import java.net.InetAddress;

public record PingResult(InetAddress address, double rtt) {
}
