package org.opennms.horizon.minion.icmp;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.opennms.horizon.shared.icmp.PingResponseCallback;
import org.opennms.horizon.shared.icmp.Pinger;

import java.net.InetAddress;
import java.util.List;

@Slf4j
@Setter
public class TestPinger implements Pinger {
    private boolean handleError = false;
    private boolean handleResponse = false;
    private boolean handleTimeout = false;

    @Override
    public void ping(InetAddress host, long timeout, int retries, int packetsize, int sequenceId, PingResponseCallback cb) throws Exception {
        ping(host, cb);
    }

    @Override
    public void ping(InetAddress host, long timeout, int retries, int sequenceId, PingResponseCallback cb) throws Exception {
        ping(host, cb);
    }

    private void ping(InetAddress host, PingResponseCallback cb) {
        if (handleError) {
            cb.handleError(host, new TestEchoPacket(false), new Exception("Failed to ping"));
        } else if (handleResponse) {
            cb.handleResponse(host, new TestEchoPacket(true));
        } else if (handleTimeout) {
            cb.handleTimeout(host, new TestEchoPacket(false));
        } else {
            throw new RuntimeException("Must set one of the responses");
        }
    }

    @Override
    public Number ping(InetAddress host, long timeout, int retries, int packetsize) throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public Number ping(InetAddress host, long timeout, int retries) throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public Number ping(InetAddress host) throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public List<Number> parallelPing(InetAddress host, int count, long timeout, long pingInterval, int size) throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public List<Number> parallelPing(InetAddress host, int count, long timeout, long pingInterval) throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public void setAllowFragmentation(boolean allow) throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public void setTrafficClass(int tc) throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public void initialize4() throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public void initialize6() throws Exception {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public boolean isV4Available() {
        throw new NotImplementedException("Not implemented for testing");
    }

    @Override
    public boolean isV6Available() {
        throw new NotImplementedException("Not implemented for testing");
    }
}
