/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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


package org.opennms.horizon.minion.flows.shell;

import lombok.Setter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Shell command to send flow data for testing purposes.
 */
@Command(scope = "opennms", name = "send-flow", description = "Sends flow data for test purposes")
@Service
@Setter
@SuppressWarnings("java:S106") // System.out is used intentionally: we want to see it in the Karaf shell
public class SendFlowCmd implements Action {

    @Option(name = "-h", aliases = "--host", description = "host to send to, default: localhost")
    String host = "localhost";

    @Option(name = "-p", aliases = "--port", description = "port to send to, default: 50000")
    int port = 50000;

    @Option(name = "-f", aliases = "--file", description = "file containing flow data, default: netflow9_test_valid01.dat")
    String file = "netflow9_test_valid01.dat";

    @Override
    public Object execute() throws Exception {

        byte[] dataToSend;
        if (Files.exists(Paths.get(file))) {
            dataToSend = Files.readAllBytes(Paths.get(file));
        } else if (this.getClass().getResource("/flows/" + file) != null) {
            dataToSend = toByteArray(this.getClass().getResourceAsStream("/flows/" + file));
        } else {
            System.out.printf("Can not read file %s. Please enter a valid file, e.g. 'netflow9_test_valid01.dat'.%n", file);
            return null;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            System.out.printf("Sending flow to the server %s:%s%n", this.host, this.port);
            InetAddress ip = InetAddress.getByName(host);
            DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length, ip, port);
            socket.send(dp);
            System.out.println("done.");
        }
        return null;
    }
}
