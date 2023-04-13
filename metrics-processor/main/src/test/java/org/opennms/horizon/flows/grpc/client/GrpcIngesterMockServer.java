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

import static org.opennms.horizon.flows.grpc.client.IngestorClientTest.getFlowDocumentsTenantIds;

import org.opennms.dataplatform.flows.ingester.v1.IngesterGrpc;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;
import lombok.Getter;


public class GrpcIngesterMockServer extends IngesterGrpc.IngesterImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcIngesterMockServer.class);

    @Getter
    private boolean flowDocumentPersisted;

    @Getter
    private String savedTenantId;

    @Override
    public void storeFlowDocuments(StoreFlowDocumentsRequest request, StreamObserver<StoreFlowDocumentsResponse> responseObserver) {
        flowDocumentPersisted = true;
        savedTenantId = getFlowDocumentsTenantIds(request);
        LOG.info("FlowDocument with tenant-id {} successfully persisted. ", savedTenantId);
        StoreFlowDocumentsResponse storeFlowDocumentResponse = StoreFlowDocumentsResponse.newBuilder().getDefaultInstanceForType();
        responseObserver.onNext(storeFlowDocumentResponse);
        responseObserver.onCompleted();
    }

}
