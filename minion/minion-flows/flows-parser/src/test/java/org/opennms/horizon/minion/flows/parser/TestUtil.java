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
package org.opennms.horizon.minion.flows.parser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Random;
import org.junit.Assert;

public class TestUtil {

    public static int findAvailablePort(int minPort, int maxPort) {
        Assert.assertTrue("'minPort' must be greater than 0", minPort > 0);
        Assert.assertTrue("'maxPort' must be greater than or equals 'minPort'", maxPort >= minPort);
        Assert.assertTrue("'maxPort' must be less than or equal to 65535", maxPort <= 65535);
        int portRange = maxPort - minPort;
        int searchCounter = 0;

        int candidatePort;
        do {
            ++searchCounter;
            if (searchCounter > portRange) {
                throw new IllegalStateException(String.format(
                        "Could not find an available UDP port in the range [%d, %d] after %d attempts",
                        minPort, maxPort, searchCounter));
            }

            candidatePort = findRandomPort(minPort, maxPort);
        } while (!isPortAvailable(candidatePort));

        return candidatePort;
    }

    private static boolean isPortAvailable(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create ServerSocket.", ex);
        }

        try {
            InetSocketAddress sa = new InetSocketAddress(port);
            serverSocket.bind(sa);
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    private static int findRandomPort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        return minPort + new Random(System.currentTimeMillis()).nextInt(portRange + 1);
    }
}
