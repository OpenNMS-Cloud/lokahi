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
package org.opennms.horizon.notifications.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;

@ExtendWith(MockitoExtension.class)
class PagerDutyEventFactoryTest {

    @Mock
    PagerDutyDao pagerDutyDao;

    @Mock
    LokahiUrlUtil lokahiUrlUtil;

    @InjectMocks
    PagerDutyEventFactory eventFactory;

    @BeforeEach
    void setUp() throws Exception {
        eventFactory.client = "OpenNMS";

        PagerDutyConfigDTO configDTO = PagerDutyConfigDTO.newBuilder()
                .setIntegrationKey("api-key")
                .setTenantId("my-tenant")
                .build();
        when(pagerDutyDao.getConfig(any())).thenReturn(configDTO);
    }

    @Test
    void eventSummaryComesFromLogMessageIfProvided() throws Exception {
        Alert alert = Alert.newBuilder()
                .setSeverity(Severity.MAJOR)
                .setUei("uei.opennms.org/generic/traps/SNMP_Cold_Start")
                // Taken from events/opennms.snmp.trap.translator.events.xml:13,
                // whitespace included.
                .setLogMessage("Agent Up with Possible Changes (coldStart Trap)\n" + "        ")
                .build();

        assertThat(eventFactory.createEvent(alert))
                .returns(
                        "Agent Up with Possible Changes (coldStart Trap)",
                        from(pagerDutyEventDTO -> pagerDutyEventDTO.getPayload().getSummary()));
    }

    @Test
    void eventSummaryComesFromUeiIfNoLogMessage() throws Exception {
        Alert alert = Alert.newBuilder()
                .setSeverity(Severity.MAJOR)
                .setUei("uei.opennms.org/generic/traps/SNMP_Cold_Start")
                .build();

        assertThat(eventFactory.createEvent(alert))
                .returns(
                        "Node Name: , Description: , Started: 1970-01-01T00:00:00Z, Policy Name: [], Rule Name: []",
                        from(pagerDutyEventDTO -> pagerDutyEventDTO.getPayload().getSummary()));
    }
}
