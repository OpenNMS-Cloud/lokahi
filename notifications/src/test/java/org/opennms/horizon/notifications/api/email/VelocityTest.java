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
package org.opennms.horizon.notifications.api.email;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.api.LokahiUrlUtil;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VelocityTest {
    @Mock
    private LokahiUrlUtil lokahiUrlUtil;

    @InjectMocks
    Velocity velocity;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(velocity, "alertTemplate", "test.txt.vm");
    }

    @Test
    void populateTemplate() {
        Alert alert = Alert.newBuilder()
                .setDescription("Server down")
                .setLogMessage("Some interesting details")
                .build();

        List<String> completedTemplate =
                velocity.populateTemplate("bossman@company", alert).lines().toList();
        assertEquals("To: bossman@company", completedTemplate.get(0));
        assertEquals("Description: " + alert.getDescription(), completedTemplate.get(1));
        assertEquals("Message: " + alert.getLogMessage(), completedTemplate.get(2));
    }
}
