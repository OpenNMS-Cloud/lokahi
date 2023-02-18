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

package org.opennms.horizon.minion.flows.shell;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.flows.parser.TelemetryRegistry;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.ParserConfig;

@Command(scope = "opennms", name = "init-flow",
    description = "manually init a flow listener for testing")
@Service
public class InitListener implements Action {

    @Reference
    TelemetryRegistry registry;

    @Option(name = "-p", aliases = "--port", description = "port to receive to, default: 50000")
    int port = 50000;

    @Option(name = "-n", aliases = "--name", description = "listener name, default: test")
    String name = "test";

    @Option(name = "-l", aliases = "--listener", description = "listener class UdpListener / TcpListener, default: UdpListener")
    String listenerClass = "UdpListener";

    @Option(name = "-p", aliases = "--parser", description = "parser class, default: Netflow5UdpParser")
    String parserClass = "Netflow5UdpParser";

    @Override
    public Object execute() {
        ListenerConfig config = ListenerConfig.newBuilder().setClassName(listenerClass).setName(name + "_listener")
            .addParsers(ParserConfig.newBuilder().setClassName(parserClass).setName(name + "_parser")).build();
        registry.createListener(config);
        System.out.println("Listener started.");
        return null;
    }

}
