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
package org.opennms.horizon.inventory.component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.discovery.PassiveDiscoveryService;
import org.opennms.horizon.shared.events.EventConstants;
import org.opennms.taskset.contract.ScanType;

@ExtendWith(MockitoExtension.class)
class NodeMonitoringManagerTest {

    @Mock
    private NodeService nodeService;

    @Mock
    private PassiveDiscoveryService passiveDiscoveryService;

    @Mock
    private PassiveDiscoveryRepository passiveDiscoveryRepository;

    @Mock
    private TagService tagService;

    @InjectMocks
    private InternalEventConsumer internalEventConsumer;

    private final String tenantId = "test-tenant";
    private Event event;
    private Node node;
    Long locationId = 5040302010L;

    private Optional<PassiveDiscovery> passiveDiscovery;

    @BeforeEach
    public void prepare() {
        event = Event.newBuilder()
                .setTenantId(tenantId)
                .setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
                .setLocationId(String.valueOf(locationId))
                .setIpAddress("127.0.0.1")
                .build();
        node = new Node();

        Tag tag = new Tag();
        tag.setName("tag1");
        ArrayList<Tag> tags = new ArrayList();
        tags.add(tag);
        PassiveDiscovery discovery = new PassiveDiscovery();
        discovery.setName("test");
        discovery.setLocationId(locationId);
        discovery.setTags(tags);
        passiveDiscovery = Optional.of(discovery);
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(passiveDiscoveryService);
    }

    @Test
    void testReceiveEventAndCreateNewNode() throws EntityExistException, LocationNotFoundException {
        doReturn(node)
                .when(nodeService)
                .createNode(any(NodeCreateDTO.class), eq(ScanType.DISCOVERY_SCAN), eq(tenantId));
        ArgumentCaptor<NodeCreateDTO> argumentCaptor = ArgumentCaptor.forClass(NodeCreateDTO.class);
        var eventLog = EventLog.newBuilder().addEvents(event);
        internalEventConsumer.consumeInternalEvents(eventLog.build().toByteArray());
        verify(nodeService).createNode(argumentCaptor.capture(), eq(ScanType.DISCOVERY_SCAN), eq(tenantId));
        verify(passiveDiscoveryService).sendNodeScan(node, null);
        verify(passiveDiscoveryService).getPassiveDiscovery(locationId, tenantId);
        NodeCreateDTO createDTO = argumentCaptor.getValue();
        assertThat(createDTO.getLocationId()).isEqualTo(event.getLocationId());
        assertThat(createDTO.getManagementIp()).isEqualTo(event.getIpAddress());
        assertThat(createDTO.getLabel()).endsWith(event.getIpAddress());
    }

    @Test
    void testReceiveEventWithDifferentUEI() {
        var anotherEvent = Event.newBuilder().setUei("something else").build();
        var eventLog = EventLog.newBuilder().addEvents(anotherEvent);
        internalEventConsumer.consumeInternalEvents(eventLog.build().toByteArray());
        verifyNoInteractions(passiveDiscoveryService);
        verifyNoInteractions(nodeService);
    }

    @Test
    void testMissingTenantID() {
        Event testEvent = Event.newBuilder()
                .setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
                .build();
        var eventLog = EventLog.newBuilder().addEvents(testEvent).build();
        assertThatThrownBy(() -> internalEventConsumer.consumeInternalEvents(eventLog.toByteArray()))
                .isInstanceOf(InventoryRuntimeException.class);
    }

    @Test
    void testEntityExistException() throws EntityExistException, LocationNotFoundException {
        doThrow(new EntityExistException("bad request"))
                .when(nodeService)
                .createNode(any(NodeCreateDTO.class), eq(ScanType.DISCOVERY_SCAN), eq(tenantId));
        ArgumentCaptor<NodeCreateDTO> argumentCaptor = ArgumentCaptor.forClass(NodeCreateDTO.class);
        var eventLog = EventLog.newBuilder().addEvents(event).build();
        internalEventConsumer.consumeInternalEvents(eventLog.toByteArray());
        verify(nodeService).createNode(argumentCaptor.capture(), eq(ScanType.DISCOVERY_SCAN), eq(tenantId));
        verify(passiveDiscoveryService).getPassiveDiscovery(locationId, tenantId);
        NodeCreateDTO createDTO = argumentCaptor.getValue();
        assertThat(createDTO.getLocationId()).isEqualTo(event.getLocationId());
        assertThat(createDTO.getManagementIp()).isEqualTo(event.getIpAddress());
        assertThat(createDTO.getLabel()).endsWith(event.getIpAddress());
    }
}
