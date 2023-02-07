/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.parser;

import com.codahale.metrics.MetricRegistry;
import org.opennms.horizon.grpc.telemetry.contract.TelemetryMessage;
import org.opennms.horizon.minion.flows.listeners.FlowsListener;
import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.parser.factory.ParserFactory;
import org.opennms.horizon.minion.flows.listeners.factory.ListenerFactory;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.sink.flows.contract.AdapterConfig;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.ParserConfig;

public interface TelemetryRegistry {
    void addListenerFactory(ListenerFactory factory);

    void addParserFactory(ParserFactory factory);

    //void addAdapterFactory(AdapterFactory factory);


    FlowsListener getListener(ListenerConfig listenerConfig);

    Parser getParser(ParserConfig parserConfig);

    FlowsListener getAdapter(AdapterConfig adapterConfig);


    MetricRegistry getMetricRegistry();

    AsyncDispatcher<TelemetryMessage> getDispatcher();

    ListenerHolder getListenerHolder();
}
