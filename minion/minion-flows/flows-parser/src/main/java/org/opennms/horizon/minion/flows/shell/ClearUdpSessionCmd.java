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

import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.parser.FlowsListenerFactory;

/**
 * Shell command to clear parsers sessions to avoid templates inconsistencies
 */
@Command(
        scope = "opennms",
        name = "clear-session",
        description = "Clear Parsers Sessions to avoid templates inconsistencies")
@Service
@SuppressWarnings("java:S106") // System.out is used intentionally: we want to see it in the Karaf shell
public class ClearUdpSessionCmd implements Action {

    @Reference
    FlowsListenerFactory.FlowsListener flowsListener;

    @Option(name = "-p", aliases = "--parserName", description = "specify udp parser name")
    String parserName = "not-specified";

    @Option(name = "-o", aliases = "--observationDomainId", description = "specify observation domain Id")
    int observationDomainId;

    @Override
    public Object execute() throws Exception {
        if (StringUtils.isBlank(parserName)) {
            System.out.println(
                    "Please specify a valid parser name, e.g. -p Netflow-5-Parser or --parserName Netflow-9-Parser");
            return null;
        }

        // Udp Sessions
        // Get Udp Parser
        final var matchedParser = flowsListener.getListeners().stream()
                .flatMap(listener -> listener.getParsers().stream())
                .filter(parser -> parserName.equals(parser.getName()))
                .findFirst();
        if (matchedParser.isEmpty()) {
            System.err.println("Parser not found: " + parserName);
            return null;
        }

        if (!UdpParser.class.isInstance(matchedParser.get())) {
            System.err.println("Parser is not a UDP parser, silly: " + parserName);
            return null;
        }

        ((UdpParser) matchedParser.get())
                .getSessionManager()
                .removeTemplateIf(
                        (e -> e.getKey().observationDomainId.observationDomainId == this.observationDomainId));

        System.out.printf("Sessions for protocol UDP and keyword %s successfully dropped.", parserName);
        return null;
    }
}
