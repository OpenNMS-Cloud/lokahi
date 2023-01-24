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

package org.opennms.horizon.events;

import jakarta.annotation.PostConstruct;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.events.conf.xml.EnterpriseIdPartition;
import org.opennms.horizon.events.conf.xml.Event;
import org.opennms.horizon.events.conf.xml.EventOrdering;
import org.opennms.horizon.events.conf.xml.Events;
import org.opennms.horizon.events.conf.xml.Partition;
import org.opennms.horizon.events.util.JaxbUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultEventConfDao implements EventConfDao {


    private final Resource configResource = new ClassPathResource("eventconf.xml", getClass().getClassLoader());

    private Map<String, Long> lastModifiedEventFiles = new LinkedHashMap<>();

    private Events events;

    private Partition partition;

    @Override
    public Event findByEvent(org.opennms.horizon.events.xml.Event matchingEvent) {
        return events.findFirstMatchingEvent(matchingEvent);
    }

    @Override
    public List<String> getEventUEIs() {
        return events.forEachEvent(new ArrayList<String>(), new Events.EventCallback<List<String>>() {
            @Override
            public List<String> process(List<String> ueis, Event event) {
                ueis.add(event.getUei());
                return ueis;
            }
        });

    }

    @PostConstruct
    public void init() {
        loadConfig();
    }

    private synchronized void loadConfig() {
        try {
            Events events = JaxbUtils.unmarshal(Events.class, configResource);
            lastModifiedEventFiles = events.loadEventFiles(configResource);

            partition = new EnterpriseIdPartition();
            events.initialize(partition, new EventOrdering());

            this.events = events;
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + configResource, e);
        }
    }
}
