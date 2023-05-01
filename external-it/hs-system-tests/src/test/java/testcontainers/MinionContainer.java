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

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import org.opennms.horizon.systemtests.CucumberHooks;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.List;

import static org.opennms.horizon.systemtests.CucumberHooks.MINIONS;
import static org.testcontainers.containers.Network.SHARED;

public class MinionContainer extends GenericContainer<MinionContainer> {
    public String gatewayHost;
    public String minionId;
    public String minionLocation;

    /*
    ## -- Karaf SSH            8101/TCP
    ## -- HTTP                 8181/TCP
    ## -- SNMP Trapd           1162/UDP
    ## -- Syslog               1514/UDP
    ## -- Flows                9999/UDP
    ## -- Flows v5             8877/UDP
    ## -- Flows v9             4729/UDP
     */
    public MinionContainer(String gatewayHost, String minionId, String minionLocation) {
        super("opennms/horizon-stream-minion:latest");
        // expose TCP ports here
        withExposedPorts(8101, 8181)
            .withNetworkAliases("horizon-stream")
            .withNetwork(SHARED)
            .withEnv("TZ", "America/New_York")
            .withEnv("MINION_ID", minionId)
            .withEnv("MINION_LOCATION", minionLocation)
            .withEnv("USE_KUBERNETES", "false")
            .withEnv("IGNITE_SERVER_ADDRESSES", "localhost")
            .withEnv("MINION_GATEWAY_HOST", gatewayHost)
            .withEnv("MINION_GATEWAY_PORT", "443")
            .withEnv("MINION_GATEWAY_TLS", "true")
            .waitingFor(
                Wait.forLogMessage(".* Udp Flow Listener started at .*", 3)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )
            .waitingFor(
                Wait.forLogMessage(".*Sending heartbeat from Minion.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(2))
            )
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

        // expose UDP ports here
        this.getPortBindings().addAll(List.of(
            ExposedPort.udp(4729).toString(),
            ExposedPort.udp(8877).toString()
        ));

        this.gatewayHost = gatewayHost;
        this.minionId = minionId.toUpperCase();
        this.minionLocation = minionLocation;
    }

    public static void createNewOne(String minionId, String minionLocation) {
        MinionContainer minionContainer = new MinionContainer(CucumberHooks.gatewayHost, minionId, minionLocation);
        minionContainer.start();
        MINIONS.add(minionContainer);
    }

    public String getUdpPortBinding(int portNumber) {
        return this.getContainerInfo().getNetworkSettings().getPorts().getBindings()
            .get(new ExposedPort(portNumber, InternetProtocol.UDP))[0].getHostPortSpec();
    }

}
