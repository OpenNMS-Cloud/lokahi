/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

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
                throw new IllegalStateException(String.format("Could not find an available UDP port in the range [%d, %d] after %d attempts", minPort, maxPort, searchCounter));
            }

            candidatePort = findRandomPort(minPort, maxPort);
        } while(!isPortAvailable(candidatePort));

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
