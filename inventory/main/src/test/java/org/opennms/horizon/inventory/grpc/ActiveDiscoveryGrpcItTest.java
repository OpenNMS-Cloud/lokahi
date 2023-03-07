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

package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.Context;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryList;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryOperationGrpc;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryRequest;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.inventory.mapper.ActiveDiscoveryMapper;
import org.opennms.horizon.inventory.repository.ActiveDiscoveryRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.repository.ConfigurationRepository;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class ActiveDiscoveryGrpcItTest extends GrpcTestBase {
    @Autowired
    private ActiveDiscoveryRepository configRepo;
    @Autowired
    private ActiveDiscoveryMapper configMapper;
    private ActiveDiscoveryOperationGrpc.ActiveDiscoveryOperationBlockingStub serviceStub;
    private final String configName = "test-config";
    private final String location = "test-location";

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = ActiveDiscoveryOperationGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        afterTest();
    }

    @Test
    void testCreateConfig() {
        SNMPConfigDTO snmpConfig = SNMPConfigDTO.newBuilder()
            .addAllPorts(List.of(161))
            .addAllReadCommunity(List.of("test")).build();
        ActiveDiscoveryRequest request = ActiveDiscoveryRequest.newBuilder()
            .setConfigName("test-config")
            .setLocation("test-location")
            .setSnmpConf(snmpConfig)
            .addAllIpAddresses(List.of("127.0.0.1-127.0.0.10")).build();

        var result = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createConfig(request);
        assertThat(result).isNotNull();

        SNMPConfigDTO snmpConfig2 = SNMPConfigDTO.newBuilder()
            .addAllPorts(List.of(1161))
            .addAllReadCommunity(List.of("test")).build();
        ActiveDiscoveryRequest request2 = ActiveDiscoveryRequest.newBuilder()
            .setConfigName("test-config2")
            .setLocation("test-location2")
            .setSnmpConf(snmpConfig2)
            .addAllIpAddresses(List.of("192.168.0.1")).build();
       var result2 = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createConfig(request2);
        assertThat(result2).isNotNull();
    }

    @Test
    void testGetConfigById() {
        ActiveDiscoveryDTO tempConfig = ActiveDiscoveryDTO.newBuilder()
            .setConfigName(configName)
            .addAllIpAddresses(List.of("127.0.0.1"))
            .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community")).build()).build();
        var model = configMapper.dtoToModel(tempConfig);
        model.setTenantId(tenantId);
        var activeDiscovery = configRepo.save(model);
        ActiveDiscoveryDTO discoveryConfig = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getDiscoveryConfigById(Int64Value.of(activeDiscovery.getId()));

        assertThat(discoveryConfig).isNotNull()
            .extracting(ActiveDiscoveryDTO::getConfigName, c -> c.getIpAddressesList().get(0), c -> c.getSnmpConf().getReadCommunityList().get(0))
            .containsExactly(configName, "127.0.0.1", "test-community");
    }

    @Test
    void testListConfig() {
        ActiveDiscoveryDTO tempConfig = ActiveDiscoveryDTO.newBuilder()
            .setConfigName(configName)
            .addAllIpAddresses(List.of("127.0.0.1"))
            .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community")).build()).build();

        ActiveDiscoveryDTO tempConfig2 = ActiveDiscoveryDTO.newBuilder()
            .setConfigName("new-config")
            .addAllIpAddresses(List.of("127.0.0.2"))
            .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community2")).build()).build();
        var config1 = configMapper.dtoToModel(tempConfig);
        var config2 = configMapper.dtoToModel(tempConfig2);
        config1.setTenantId(tenantId);
        config2.setTenantId(tenantId);
        configRepo.saveAll(List.of(config1, config2));

        ActiveDiscoveryList result = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listDiscoveryConfig(Empty.getDefaultInstance());

        assertThat(result).isNotNull()
            .extracting(ActiveDiscoveryList::getDiscoverConfigsList).asList().hasSize(2)
            .extracting("configName")
            .contains(configName, "new-config");
    }

}
