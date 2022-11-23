package org.opennms.horizon.minion.icmp;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.shared.icmp.EchoPacket;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TestEchoPacket implements EchoPacket {
    private final boolean echoReply;

    @Override
    public boolean isEchoReply() {
        return this.echoReply;
    }

    @Override
    public int getIdentifier() {
        return 1;
    }

    @Override
    public int getSequenceNumber() {
        return 1;
    }

    @Override
    public long getThreadId() {
        return Thread.currentThread().getId();
    }

    @Override
    public long getReceivedTimeNanos() {
        return 1000;
    }

    @Override
    public long getSentTimeNanos() {
        return 1000;
    }

    @Override
    public double elapsedTime(TimeUnit timeUnit) {
        return 1000;
    }
}
