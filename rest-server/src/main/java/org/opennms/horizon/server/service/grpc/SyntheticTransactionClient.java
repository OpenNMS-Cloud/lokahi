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

package org.opennms.horizon.server.service.grpc;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.opennms.horizon.inventory.dto.SyntheticTestCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestLocationConfigurationDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestPluginConfigurationDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestStatusDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestStatusMapDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTOArray;
import org.opennms.horizon.inventory.dto.SyntheticTransactionRequestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionServiceGrpc;
import org.opennms.horizon.inventory.dto.SyntheticTransactionServiceGrpc.SyntheticTransactionServiceBlockingStub;
import org.opennms.horizon.inventory.dto.SyntheticTransactionTestDTOArray;
import org.opennms.horizon.inventory.dto.SyntheticTransactionTestLocationRequestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionTestRequestDTO;
import org.opennms.horizon.inventory.dto.TenantedId;
import org.opennms.horizon.shared.constants.GrpcConstants;

public class SyntheticTransactionClient {

    private final long deadline;

    private final SyntheticTransactionServiceBlockingStub syntheticTransactionServiceStub;

    public SyntheticTransactionClient(ManagedChannel channel, long deadline) {
        this.deadline = deadline;
        this.syntheticTransactionServiceStub = SyntheticTransactionServiceGrpc.newBlockingStub(channel);
    }

    public List<SyntheticTransactionDTO> getTransactions(String accessToken) {
        SyntheticTransactionDTOArray array = call(stub -> stub.getSyntheticTransactions(Empty.getDefaultInstance()), accessToken);

        return array.getTransactionsList();
    }

    public List<SyntheticTestDTO> getSyntheticTests(String tenantId, long syntheticTransactionId, String authHeader) {
        SyntheticTransactionTestDTOArray syntheticTestArray = call(stub ->
            stub.getSyntheticTransactionTests(SyntheticTransactionRequestDTO.newBuilder()
                .setId(TenantedId.newBuilder().setTenant(tenantId).setId(syntheticTransactionId).build())
                .build()
            ),
            authHeader
        );
        return syntheticTestArray.getTestsList();
    }

    public SyntheticTransactionDTO createSyntheticTransaction(SyntheticTransactionCreateDTO request, String accessToken) {
        return call(stub -> stub.createSyntheticTransaction(request), accessToken);
    }

    public SyntheticTestDTO createSyntheticTest(SyntheticTestCreateDTO request, String accessToken) {
        return call(stub -> stub.createSyntheticTransactionTest(request), accessToken);
    }

    public SyntheticTestStatusMapDTO executeTest(String tenantId, long testId, String accessToken) {
        return call(stub ->
            stub.executeSyntheticTest(SyntheticTransactionTestRequestDTO.newBuilder()
                .setId(TenantedId.newBuilder().setTenant(tenantId).setId(testId).build())
                .build()
            ),
            accessToken
        );
    }

    public SyntheticTestStatusDTO executeTestInLocation(String tenantId, long testId, String location, String authHeader) {
        return call(stub ->
            stub.executeSyntheticTestInLocation(SyntheticTransactionTestLocationRequestDTO.newBuilder()
                .setLocation(location)
                .setId(TenantedId.newBuilder().setTenant(tenantId).setId(testId).build())
                .build()
            ),
            authHeader
        );
    }

    public SyntheticTestStatusDTO verifyTestInLocation(String tenantId, SyntheticTestPluginConfigurationDTO configuration, String location, String authHeader) {
        return call(stub ->
            stub.verifySyntheticTestConfiguration(SyntheticTestLocationConfigurationDTO.newBuilder()
                .setLocation(location)
                .setPluginConfiguration(configuration)
                .build()
            ),
            authHeader
        );
    }

    /**
      * Wrap given call and attach authorization header and deadline parameters.
      */
    private <T> T call(Function<SyntheticTransactionServiceBlockingStub, T> call, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        SyntheticTransactionServiceBlockingStub invocationStub = syntheticTransactionServiceStub.withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
        return call.apply(invocationStub);
    }
}
