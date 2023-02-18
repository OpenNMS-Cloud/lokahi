/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ConfigManager implements org.opennms.horizon.minion.plugin.api.ListenerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private final TelemetryRegistry telemetryRegistry;

    private FlowsConfig flowsConfig;

    public ConfigManager(final TelemetryRegistry telemetryRegistry) {
        this.telemetryRegistry = Objects.requireNonNull(telemetryRegistry);
    }

    @Override
    public Listener create(Any config) {
        LOG.info("FlowsConfig: {}", config.toString());

        if (!config.is(FlowsConfig.class)) {
            throw new IllegalArgumentException("configuration must be FlowsConfig; type-url=" + config.getTypeUrl());
        }
        var holder = telemetryRegistry.getListenerHolder();
        try {
            this.flowsConfig = config.unpack(FlowsConfig.class);
            holder.clear();
            flowsConfig.getListenersList().forEach(telemetryRegistry::createListener);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Error while parsing config with type-url=" + config.getTypeUrl());
        }
        return holder;
    }
}
