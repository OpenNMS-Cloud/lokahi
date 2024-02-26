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

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.opennms.dataplatform.flows.ingester.v1.IngesterGrpc;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;

@RequiredArgsConstructor
public class IngestorClient {

    private final ManagedChannel channel;
    private final long deadline;
    private final RetryTemplate retryTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(IngestorClient.class);

    @Setter
    private IngesterGrpc.IngesterBlockingStub ingesterBlockingStub;

    public void initStubs() {
        ingesterBlockingStub = IngesterGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (Objects.nonNull(channel) && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    private Metadata getMetadata(boolean bypassAuthorization, String tenantId) {
        var metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(bypassAuthorization));
        // TODO: Still not sure if the tenant id(s) is needed in the metadata, to be clarified
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        return metadata;
    }

    public void sendData(StoreFlowDocumentsRequest storeFlowDocumentsRequest, String tenantId) {
        Metadata metadata = getMetadata(true, tenantId);
        try {
            retryTemplate.execute(context -> {
                LOG.debug(
                        "Attempt number {} to persist StoreFlowDocumentRequest for tenantId {}. ",
                        context.getRetryCount() + 1,
                        tenantId);
                ingesterBlockingStub
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                        .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                        .storeFlowDocuments(storeFlowDocumentsRequest);
                LOG.debug("FlowDocuments successfully persisted.");
                return true;
            });
        } catch (RuntimeException e) {
            LOG.error("Failed to send StoreFlowDocumentRequest for tenant-id {} to FlowIngestor. ", tenantId, e);
            throw e;
        }
    }
}
