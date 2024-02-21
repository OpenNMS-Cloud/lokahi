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

import static org.opennms.horizon.flows.grpc.client.IngestorClientTest.getFlowDocumentsTenantIds;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import org.opennms.dataplatform.flows.ingester.v1.IngesterGrpc;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcIngesterMockServer extends IngesterGrpc.IngesterImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcIngesterMockServer.class);

    @Getter
    private boolean flowDocumentPersisted;

    @Getter
    private String savedTenantId;

    @Override
    public void storeFlowDocuments(
            StoreFlowDocumentsRequest request, StreamObserver<StoreFlowDocumentsResponse> responseObserver) {
        flowDocumentPersisted = true;
        savedTenantId = getFlowDocumentsTenantIds(request);
        LOG.info("FlowDocument with tenant-id {} successfully persisted. ", savedTenantId);
        StoreFlowDocumentsResponse storeFlowDocumentResponse =
                StoreFlowDocumentsResponse.newBuilder().getDefaultInstanceForType();
        responseObserver.onNext(storeFlowDocumentResponse);
        responseObserver.onCompleted();
    }
}
