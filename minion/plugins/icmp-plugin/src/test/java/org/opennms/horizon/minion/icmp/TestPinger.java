/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.icmp;

import java.net.InetAddress;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.opennms.horizon.shared.icmp.PingResponseCallback;
import org.opennms.horizon.shared.icmp.Pinger;

@Slf4j
@Setter
public class TestPinger implements Pinger {
    private boolean handleError = false;
    private boolean handleResponse = false;
    private boolean handleTimeout = false;

    @Override
    public void ping(
            InetAddress host, long timeout, int retries, int packetsize, int sequenceId, PingResponseCallback cb)
            throws Exception {
        ping(host, cb);
    }

    @Override
    public void ping(InetAddress host, long timeout, int retries, int sequenceId, PingResponseCallback cb)
            throws Exception {
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
    public List<Number> parallelPing(InetAddress host, int count, long timeout, long pingInterval, int size)
            throws Exception {
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
