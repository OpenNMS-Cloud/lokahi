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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.conf.xml.Event;

public class EventConfTest {

    @Test
    public void testEventConf() {
        DefaultEventConfDao eventConfDao = new DefaultEventConfDao();
        eventConfDao.init();
        var ueis = eventConfDao.getEventUEIs();
        assertFalse(ueis.isEmpty(), "Should have loaded some ueis");
        String uei = "uei.opennms.org/generic/traps/SNMP_Cold_Start";
        EventBuilder eb = new EventBuilder(uei, "JUnit");
        Event event = eventConfDao.findByEvent(eb.getEvent());
        assertNotNull(event);
        assertEquals(uei, event.getUei());
        assertEquals("Normal", event.getSeverity());
        var events = eventConfDao.getAllEventsByUEI();
        System.out.printf("size of events = %d ", events.size());
        AtomicInteger countOfEventsWithVendor = new AtomicInteger(0);
        AtomicInteger sizeOfEventsWithAlarmData = new AtomicInteger(0);
        events.forEach((eventUEI, eventConf) -> {
            String eventUei = eventConf.getUei();
            String enterpriseId = null;
            var enterpriseIds = eventConf.getMaskElementValues("id");
            if (enterpriseIds != null && enterpriseIds.size() == 1) {
                enterpriseId = enterpriseIds.get(0);
                // System.out.println("Enterprise id = " + enterpriseId);
            }
            String vendor = extractVendorFromUei(eventUei);
            if (eventConf.getAlertData() != null) {
                String reductionKey = eventConf.getAlertData().getReductionKey();
                String clearKey = eventConf.getAlertData().getClearKey();
                Integer alerttype = eventConf.getAlertData().getAlertType();
                sizeOfEventsWithAlarmData.incrementAndGet();
            }
            if (vendor != null & enterpriseId != null) {
                countOfEventsWithVendor.incrementAndGet();
            }
        });
        System.out.printf("size of events with vendor = %d \n", countOfEventsWithVendor.get());
        System.out.printf("size of events with alarmdata = %d \n", sizeOfEventsWithAlarmData.get());
    }

    private String extractVendorFromUei(String eventUei) {

        if (eventUei.contains("vendor")) {
            Pattern pattern = Pattern.compile("/vendor(s?)/([^/]+)/");
            Matcher matcher = pattern.matcher(eventUei);
            if (matcher.find()) {
                // Extract word immediately after "vendor" or "vendors" within slashes
                return matcher.group(2);
            } else {
                throw new IllegalArgumentException("No match found for " + eventUei);
            }
        } else if (eventUei.contains("trap")) {
            Pattern pattern = Pattern.compile("/traps/([^/]+)/");
            Matcher matcher = pattern.matcher(eventUei);
            if (matcher.find()) {
                // Extract word immediately after "traps" within slashes
                return matcher.group(1);
            } else {
                Pattern patternForTraps = Pattern.compile("uei\\.opennms\\.org/(.*?)/traps/");
                Matcher matcherForTraps = patternForTraps.matcher(eventUei);
                if (matcherForTraps.find()) {
                    // Extract the string between "uei.opennms.org" and "/traps/"
                    return matcherForTraps.group(1);
                }
            }
        }
        return null;
    }
}
