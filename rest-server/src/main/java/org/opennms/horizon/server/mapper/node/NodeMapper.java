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

package org.opennms.horizon.server.mapper.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.AzureNodeDTO;
import org.opennms.horizon.inventory.dto.DefaultNodeDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.inventory.node.Node;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NodeMapper {
    private static final String DEFAULT_NODE_TYPE = "DEFAULT";
    private static final String AZURE_NODE_TYPE = "AZURE";
    private final DefaultNodeMapper defaultNodeMapper;
    private final AzureNodeMapper azureNodeMapper;
    private final ObjectMapper objectMapper;

    public Node dtoToNode(NodeDTO dto) {
        if (dto.hasDefault()) {
            return defaultDtoToNode(dto.getDefault());
        } else if (dto.hasAzure()) {
            return azureDtoToNode(dto.getAzure());
        }
        throw new RuntimeException("Invalid Node type returned");
    }

    public Node defaultDtoToNode(DefaultNodeDTO dto) {
        Node node = new Node();
        node.setDetails(objectMapper.valueToTree(defaultNodeMapper.protoToNode(dto)));
        node.setNodeType(DEFAULT_NODE_TYPE);
        return node;
    }

    private Node azureDtoToNode(AzureNodeDTO dto) {
        Node node = new Node();
        node.setDetails(objectMapper.valueToTree(azureNodeMapper.protoToNode(dto)));
        node.setNodeType(AZURE_NODE_TYPE);
        return node;
    }
}
