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
package org.opennms.horizon.events;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

@Component
public class DefaultEventConfDao implements EventConfDao {

    private final Resource configResource =
            new ClassPathResource("eventconf.xml", getClass().getClassLoader());

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

    @Override
    public Map<String, Event> getAllEventsByUEI() {
        return events.getAllEventsByUei();
    }
}
