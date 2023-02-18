package org.opennms.horizon.minion.icmp;

import java.net.InetAddress;

public record IPPollAddress(InetAddress address, long timeout, int retries) {
}
