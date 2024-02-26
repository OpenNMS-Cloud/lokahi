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
package org.opennms.horizon.minion.flows.shell;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.flows.parser.TelemetryRegistry;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.ParserConfig;

@Command(scope = "opennms", name = "init-flow", description = "manually init a flow listener for testing")
@Service
public class InitListener implements Action {

    @Reference
    TelemetryRegistry registry;

    @Option(name = "-p", aliases = "--port", description = "port to receive to, default: 50000")
    int port = 50000;

    @Option(name = "-n", aliases = "--name", description = "listener name, default: test")
    String name = "test";

    @Option(
            name = "-l",
            aliases = "--listener",
            description = "listener class UdpListener / TcpListener, default: UdpListener")
    String listenerClass = "UdpListener";

    @Option(name = "-p", aliases = "--parser", description = "parser class, default: Netflow5UdpParser")
    String parserClass = "Netflow5UdpParser";

    @Override
    public Object execute() {
        ListenerConfig config = ListenerConfig.newBuilder()
                .setClassName(listenerClass)
                .setName(name + "_listener")
                .addParsers(ParserConfig.newBuilder().setClassName(parserClass).setName(name + "_parser"))
                .build();
        registry.createListener(config);
        System.out.println("Listener started.");
        return null;
    }
}
