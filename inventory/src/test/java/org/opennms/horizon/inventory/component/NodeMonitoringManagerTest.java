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

package org.opennms.horizon.inventory.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.events.EventConstants;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NodeMonitoringManagerTest {

    @Mock
    private NodeService nodeService;
    @Mock
    private DetectorTaskSetService detectorService;
    @InjectMocks
    private NodeMonitoringManager consumer;

    private final String tenantId = "test-tenant";
    private Event event;
    private Map<String, Object> headers;
    private Node node;

    @BeforeEach
    public void prepare(){
        event = Event.newBuilder()
            .setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
            .setLocation("test-location")
            .setIpAddress("127.0.0.1")
            .build();
        headers = new HashMap<>();
        headers.put(GrpcConstants.TENANT_ID_KEY, tenantId.getBytes(StandardCharsets.UTF_8));
        node = new Node();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(nodeService);
        verifyNoMoreInteractions(detectorService);
    }

    @Test
    void testReceiveEventAndCreateNewNode() {
        doReturn(node).when(nodeService).createNode(any(NodeCreateDTO.class), eq(tenantId));
        ArgumentCaptor<NodeCreateDTO> argumentCaptor = ArgumentCaptor.forClass(NodeCreateDTO.class);
        consumer.receiveTrapEvent(event.toByteArray(), headers);
        verify(nodeService).createNode(argumentCaptor.capture(), eq(tenantId));
        NodeCreateDTO createDTO = argumentCaptor.getValue();
        assertThat(createDTO.getLocation()).isEqualTo(event.getLocation());
        assertThat(createDTO.getManagementIp()).isEqualTo(event.getIpAddress());
        assertThat(createDTO.getLabel()).endsWith(event.getIpAddress());
        verify(detectorService, timeout(10000)).sendDetectorTasks(any(Node.class));
    }

    @Test
    void testReceiveEventWithDifferentUEI() {
        var anotherEvent = Event.newBuilder()
                .setUei("something else").build();
        consumer.receiveTrapEvent(anotherEvent.toByteArray(), headers);
        verifyNoInteractions(detectorService);
        verifyNoInteractions(nodeService);
    }
}
