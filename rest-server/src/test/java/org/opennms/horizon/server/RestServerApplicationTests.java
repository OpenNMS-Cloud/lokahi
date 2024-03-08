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
import org.opennms.horizon.server.service.GrpcAlertService;
import org.opennms.horizon.server.service.GrpcEventService;
import org.opennms.horizon.server.service.GrpcLocationService;
import org.opennms.horizon.server.service.GrpcMinionService;
import org.opennms.horizon.server.service.GrpcNodeService;
import org.opennms.horizon.server.service.NotificationService;
import org.opennms.horizon.server.service.discovery.GrpcAzureActiveDiscoveryService;
import org.opennms.horizon.server.service.flows.GrpcFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RestServerApplicationTests {
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private GrpcMinionService grpcMinionService;

    @Autowired
    private GrpcEventService grpcEventService;

    @Autowired
    private GrpcNodeService grpcNodeService;

    @Autowired
    private GrpcLocationService grpcLocationService;

    @Autowired
    private GrpcAlertService grpcAlertService;

    @Autowired
    private GrpcAzureActiveDiscoveryService grpcAzureActiveDiscoveryService;

    @Autowired
    private GrpcFlowService grpcFlowService;

    @Test
    void contextLoads() {
        assertNotNull(grpcMinionService);
        assertNotNull(notificationService);
        assertNotNull(grpcLocationService);
        assertNotNull(grpcEventService);
        assertNotNull(grpcNodeService);
        assertNotNull(grpcAlertService);
        assertNotNull(grpcAzureActiveDiscoveryService);
        assertNotNull(grpcFlowService);
    }
}
