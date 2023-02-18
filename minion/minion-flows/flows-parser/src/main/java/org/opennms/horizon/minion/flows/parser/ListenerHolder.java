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

import org.opennms.horizon.minion.flows.listeners.FlowsListener;
import org.opennms.horizon.minion.plugin.api.Listener;

import java.util.HashMap;
import java.util.Map;

public class ListenerHolder implements Listener {

    private final Map<String, FlowsListener> listenerMap = new HashMap<>();

    public void clear() {
        stop(); // stop all before remove prevent resource leakage
        listenerMap.clear();
    }

    public int size() {
        return listenerMap.size();
    }

    public FlowsListener get(String name) {
        return this.listenerMap.get(name);
    }

    public void put(FlowsListener listener) {
        this.listenerMap.put(listener.getName(), listener);
    }

    public FlowsListener remove(String name) {
        return this.listenerMap.remove(name);
    }

    @Override
    public void start() throws Exception {
        for (FlowsListener listener : listenerMap.values()) {
            listener.start();
        }
    }

    @Override
    public void stop() {
        listenerMap.values().forEach(FlowsListener::stop);
    }
}
