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
package org.opennms.netmgt.syslogd;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opennms.horizon.shared.utils.InetAddressUtils.addr;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;




import com.google.common.util.concurrent.RateLimiter;

import io.netty.util.ResourceLeakDetector;
import org.opennms.horizon.grpc.syslog.contract.SyslogConnection;
import org.opennms.horizon.minion.syslog.listener.SyslogReceiverCamelNettyImpl;
import org.opennms.horizon.minion.syslog.listener.SyslogdConfig;

public class SyslogdReceiverCamelNettyIT {

    public static final String NETTY_LEAK_DETECTION_LEVEL = "io.netty.leakDetectionLevel";

    private static String s_oldLeakLevel;

    @BeforeClass
    public static void setSerializablePackages() {
        s_oldLeakLevel = System.getProperty(NETTY_LEAK_DETECTION_LEVEL);
        System.setProperty(NETTY_LEAK_DETECTION_LEVEL, ResourceLeakDetector.Level.PARANOID.toString());
    }

    @AfterClass
    public static void resetSerializablePackages() {
        if (s_oldLeakLevel == null) {
            System.clearProperty(NETTY_LEAK_DETECTION_LEVEL);
        } else {
            System.setProperty(NETTY_LEAK_DETECTION_LEVEL, s_oldLeakLevel);
        }
    }



    @Test(timeout=3 * 60 * 1000)
    public void testParallelismAndQueueing() throws UnknownHostException, InterruptedException, ExecutionException {
        final int NUM_GENERATORS = 3;
        final double MESSAGE_RATE_PER_GENERATOR = 1000.0;
        final int NUM_CONSUMER_THREADS = 8;
        final int MESSAGE_QUEUE_SIZE = 529;



        SyslogdConfig syslogdConfig = mock(SyslogdConfig.class);

        when(syslogdConfig.getNumThreads()).thenReturn(NUM_CONSUMER_THREADS);
        when(syslogdConfig.getQueueSize()).thenReturn(MESSAGE_QUEUE_SIZE);


        SyslogReceiverCamelNettyImpl syslogReceiver = new SyslogReceiverCamelNettyImpl(syslogdConfig);

        syslogReceiver.run();





        // Now all of the threads are locked, and the queue is full
        // Let's continue generating traffic for a few seconds
        Thread.sleep(SECONDS.toMillis(10));



    }


}
