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
package org.opennms.horizon.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.service.GraphQLAlertService;
import org.opennms.horizon.server.service.GraphQLEventService;
import org.opennms.horizon.server.service.GraphQLLocationService;
import org.opennms.horizon.server.service.GraphQLMinionService;
import org.opennms.horizon.server.service.GraphQLNodeService;
import org.opennms.horizon.server.service.GraphQLNotificationService;
import org.opennms.horizon.server.service.discovery.GraphQLAzureActiveDiscoveryService;
import org.opennms.horizon.server.service.flows.GraphQLFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RestServerApplicationTests {
    @Autowired
    private GraphQLNotificationService graphQLNotificationService;

    @Autowired
    private GraphQLMinionService graphQLMinionService;

    @Autowired
    private GraphQLEventService graphQLEventService;

    @Autowired
    private GraphQLNodeService graphQLNodeService;

    @Autowired
    private GraphQLLocationService graphQLLocationService;

    @Autowired
    private GraphQLAlertService graphQLAlertService;

    @Autowired
    private GraphQLAzureActiveDiscoveryService graphQLAzureActiveDiscoveryService;

    @Autowired
    private GraphQLFlowService graphQLFlowService;

    @Test
    void contextLoads() {
        assertNotNull(graphQLMinionService);
        assertNotNull(graphQLNotificationService);
        assertNotNull(graphQLLocationService);
        assertNotNull(graphQLEventService);
        assertNotNull(graphQLNodeService);
        assertNotNull(graphQLAlertService);
        assertNotNull(graphQLAzureActiveDiscoveryService);
        assertNotNull(graphQLFlowService);
    }
}
