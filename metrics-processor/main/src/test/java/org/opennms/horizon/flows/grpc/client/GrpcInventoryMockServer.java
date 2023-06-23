/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.grpc.client;

import java.util.HashMap;
import java.util.Map;

import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;


public class GrpcInventoryMockServer extends NodeServiceGrpc.NodeServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcInventoryMockServer.class);

    @Getter
    private final Map<String, NodeIdQuery> incomingNodeIdQueries = new HashMap<>();

    @Override
    public void getIpInterfaceFromQuery(org.opennms.horizon.inventory.dto.NodeIdQuery request,
                                        io.grpc.stub.StreamObserver<org.opennms.horizon.inventory.dto.IpInterfaceDTO> responseObserver) {
        LOG.info("Getting Ip interface from Query.. ");
        incomingNodeIdQueries.put(request.getLocationId(), request);
        responseObserver.onNext(IpInterfaceDTO.newBuilder().build());
        responseObserver.onCompleted();
    }

}
