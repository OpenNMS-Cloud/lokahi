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
package org.opennms.horizon.flows.grpc.client;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcInventoryMockServer extends NodeServiceGrpc.NodeServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcInventoryMockServer.class);

    @Getter
    private final Map<String, NodeIdQuery> incomingNodeIdQueries = new HashMap<>();

    @Override
    public void getIpInterfaceFromQuery(
            org.opennms.horizon.inventory.dto.NodeIdQuery request,
            io.grpc.stub.StreamObserver<org.opennms.horizon.inventory.dto.IpInterfaceDTO> responseObserver) {
        LOG.info("Getting Ip interface from Query.. ");
        incomingNodeIdQueries.put(request.getLocationId(), request);
        responseObserver.onNext(IpInterfaceDTO.newBuilder().build());
        responseObserver.onCompleted();
    }
}
