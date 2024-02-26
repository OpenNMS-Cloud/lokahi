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
package org.opennms.horizon.minion.jicmp.standalone;

import static org.junit.Assume.assumeTrue;

import com.sun.jna.Platform;
import java.net.Inet4Address;
import java.net.InetAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * PingThreadTest
 *
 * @author brozow
 */
@Ignore
public class PingThreadIT {

    @Before
    public void setUp() throws Exception {
        assumeTrue(Platform.isMac());
    }

    @After
    public void tearDown() {}

    @Test
    public void testMultiThreadSocketUse() throws Exception {
        int pingCount = 10;
        V4Pinger listener = new V4Pinger(1234);
        try {
            listener.start();

            listener.ping((Inet4Address) InetAddress.getByName("127.0.0.1"), 1000, 0, pingCount, 1000);

        } finally {
            listener.stop();
            listener.closeSocket();
        }
    }

    @Test
    public void testManyThreadSocketUse() throws Exception {
        V4Pinger listener = new V4Pinger(1243);
        try {
            listener.start();

            Thread t1 = pingThead(listener, 1000, 5);
            Thread t2 = pingThead(listener, 2000, 5);
            Thread t3 = pingThead(listener, 3000, 5);
            Thread t4 = pingThead(listener, 4000, 5);
            Thread t5 = pingThead(listener, 5000, 5);

            t1.start();
            t2.start();
            t3.start();
            t4.start();
            t5.start();

            t1.join();
            t2.join();
            t3.join();
            t4.join();
            t5.join();

        } finally {
            listener.stop();
            listener.closeSocket();
        }
    }

    private Thread pingThead(final V4Pinger listener, final int id, final int count) {
        return new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(id / 10);
                    listener.ping((Inet4Address) InetAddress.getByName("127.0.0.1"), id, 0, count, 1000);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
