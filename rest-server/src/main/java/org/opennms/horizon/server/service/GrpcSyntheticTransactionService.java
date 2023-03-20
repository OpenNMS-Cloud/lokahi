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

package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.SyntheticTestCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestStatusDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestStatusMapDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTO;
import org.opennms.horizon.inventory.dto.TenantedId;
import org.opennms.horizon.server.mapper.SyntheticTransactionPluginConfigurationMapper;
import org.opennms.horizon.server.mapper.SyntheticTransactionTestMapper;
import org.opennms.horizon.server.mapper.SyntheticTransactionMapper;
import org.opennms.horizon.server.mapper.SyntheticTransactionTestStatusMapper;
import org.opennms.horizon.server.model.inventory.st.CreateSyntheticTransactionTest;
import org.opennms.horizon.server.model.inventory.st.SyntheticTransactionTestPluginConfiguration;
import org.opennms.horizon.server.model.inventory.st.SyntheticTransactionTest;
import org.opennms.horizon.server.model.inventory.st.SyntheticTransaction;
import org.opennms.horizon.server.model.inventory.st.SyntheticTransactionTestStatus;
import org.opennms.horizon.server.model.validation.DataValidator;
import org.opennms.horizon.server.service.grpc.SyntheticTransactionClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcSyntheticTransactionService {
    private final SyntheticTransactionClient client;

    private final DataValidator validator;

    private final SyntheticTransactionTestMapper syntheticTestMapper;
    private final SyntheticTransactionMapper syntheticTransactionMapper;

    private final SyntheticTransactionTestStatusMapper statusMapper;

    private final SyntheticTransactionPluginConfigurationMapper pluginMapper;

    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<SyntheticTransaction> findSyntheticTransactions(@GraphQLEnvironment ResolutionEnvironment env) {
        List<SyntheticTransactionDTO> transactions = client.getTransactions(headerUtil.getAuthHeader(env));
        return Flux.fromStream(transactions.stream().map(syntheticTransactionMapper::fromProtobuf));
    }

    @GraphQLQuery
    public Flux<SyntheticTransactionTest> findSyntheticTransactionTests(@GraphQLArgument(name = "syntheticTransactionId") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        List<SyntheticTestDTO> syntheticTests = client.getSyntheticTests(headerUtil.extractTenant(env), id, headerUtil.getAuthHeader(env));
        return Flux.fromStream(syntheticTests.stream().map(syntheticTestMapper::fromProtobuf));
    }

    @GraphQLMutation
    public Mono<SyntheticTransaction> createSyntheticTransaction(@NotBlank String label, @GraphQLEnvironment ResolutionEnvironment env) {
        if (label.isBlank()) {
            return Mono.error(new IllegalArgumentException("Missing transaction label"));
        }

        SyntheticTransactionCreateDTO request = SyntheticTransactionCreateDTO.newBuilder()
            .setTenantId(headerUtil.extractTenant(env))
            .setLabel(label)
            .build();
        return Mono.just(client.createSyntheticTransaction(request, headerUtil.getAuthHeader(env)))
            .map(syntheticTransactionMapper::fromProtobuf);
    }

    @GraphQLMutation
    public Mono<SyntheticTransactionTest> createSyntheticTransactionTest(CreateSyntheticTransactionTest test, @GraphQLEnvironment ResolutionEnvironment env) {
        Set<ConstraintViolation<CreateSyntheticTransactionTest>> violations = validator.validate(test);
        if (!violations.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Invalid input " + violations));
        }
        SyntheticTestCreateDTO request = syntheticTestMapper.createRequest(test).toBuilder()
            .setSyntheticTransactionId(TenantedId.newBuilder().setTenant(headerUtil.extractTenant(env))
                .setId(test.getSyntheticTransactionId()).build())
            .build();
        return Mono.just(client.createSyntheticTest(request, headerUtil.getAuthHeader(env)))
            .map(syntheticTestMapper::fromProtobuf);
    }

    @GraphQLMutation
    public Mono<Map<String, SyntheticTransactionTestStatus>> executeTest(@GraphQLArgument(name = "syntheticTestId") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        SyntheticTestStatusMapDTO statusMap = client.executeTest(headerUtil.extractTenant(env), id, headerUtil.getAuthHeader(env));
        return Mono.just(statusMap)
            .map(map -> statusMap.getStatusMapMap().entrySet().stream()
                .collect(Collectors.toMap(
                    Entry::getKey, entry -> statusMapper.fromProtobuf(entry.getValue())
                ))
            );
    }

    @GraphQLMutation
    public Mono<SyntheticTransactionTestStatus> executeTestInLocation(@GraphQLArgument(name = "syntheticTestId") long id, @GraphQLArgument(name = "location") String location, @GraphQLEnvironment ResolutionEnvironment env) {
        SyntheticTestStatusDTO syntheticTests = client.executeTestInLocation(headerUtil.extractTenant(env), id, location, headerUtil.getAuthHeader(env));
        return Mono.just(syntheticTests)
            .map(statusMapper::fromProtobuf);
    }

    @GraphQLMutation
    public Mono<SyntheticTransactionTestStatus> verifyConfigurationInLocation(@GraphQLArgument(name = "configuration") SyntheticTransactionTestPluginConfiguration configuration, @GraphQLArgument(name = "location") String location, @GraphQLEnvironment ResolutionEnvironment env) {
        Set<ConstraintViolation<SyntheticTransactionTestPluginConfiguration>> violations = validator.validate(configuration);
        if (!violations.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Invalid input " + violations));
        }

        SyntheticTestStatusDTO syntheticTests = client.verifyTestInLocation(headerUtil.extractTenant(env), pluginMapper.toProtobuf(configuration), location, headerUtil.getAuthHeader(env));
        return Mono.just(syntheticTests)
            .map(statusMapper::fromProtobuf);
    }

}
