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

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.parser.FlowsListenerFactory;

/**
 * Shell command to clear parsers sessions to avoid templates inconsistencies
 */
@Command(scope = "opennms", name = "clear-session", description = "Clear Parsers Sessions to avoid templates inconsistencies")
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
            System.out.println("Please specify a valid parser name, e.g. -p Netflow-5-Parser or --parserName Netflow-9-Parser");
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

        ((UdpParser) matchedParser.get()).getSessionManager().removeTemplateIf((e ->
            e.getKey().observationDomainId.observationDomainId == this.observationDomainId));

        System.out.printf("Sessions for protocol UDP and keyword %s successfully dropped.", parserName);
        return null;
    }
}
