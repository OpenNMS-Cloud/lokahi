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

package testcontainers;

import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetAddress;
import java.time.Duration;

public class UpdGenContainer extends GenericContainer<UpdGenContainer> {
    @SneakyThrows
    public UpdGenContainer(Object port, String flowType, Object numberOfPackages) {
        super("opennms/udpgen:latest");
        withNetwork(Network.SHARED)
            .withNetworkAliases("horizon-stream")
            .withCommand("/udpgen",
                "-h", InetAddress.getLocalHost().getHostAddress(),
                "-p", port.toString(),
                "-x", flowType,
                "-r", "10", // packages per second
                "-s", numberOfPackages.toString()
            ).withLogConsumer(
                new Slf4jLogConsumer(LoggerFactory.getLogger(UpdGenContainer.class))
            )
            .waitingFor(
                Wait.forLogMessage(".*Sent " + numberOfPackages + " packets.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(Integer.parseInt(numberOfPackages.toString()) / 10 + 60))
            );
    }
}
