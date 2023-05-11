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

package org.opennms.horizon.inventory.grpc.discovery;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryList;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.inventory.grpc.GrpcTestBase;
import org.opennms.horizon.inventory.mapper.discovery.IcmpActiveDiscoveryMapper;
import org.opennms.horizon.inventory.repository.discovery.active.IcmpActiveDiscoveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@AutoConfigureObservability     // Make sure to include Metrics (for some reason they are disabled by default in the integration grey-box test)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled
class IcmpActiveDiscoveryGrpcItTest extends GrpcTestBase {
    @Autowired
    private IcmpActiveDiscoveryRepository repository;
    @Autowired
    private IcmpActiveDiscoveryMapper mapper;
    private IcmpActiveDiscoveryServiceGrpc.IcmpActiveDiscoveryServiceBlockingStub serviceStub;
    private static final String TEST_NAME = "test-config";

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = IcmpActiveDiscoveryServiceGrpc.newBlockingStub(channel);
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
        IcmpActiveDiscoveryCreateDTO request = IcmpActiveDiscoveryCreateDTO.newBuilder()
            .setName("test-config")
            .setLocation("test-location")
            .setSnmpConf(snmpConfig)
            .addAllIpAddresses(List.of("127.0.0.1-127.0.0.10")).build();

        var result = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createDiscovery(request);
        assertThat(result).isNotNull();

        SNMPConfigDTO snmpConfig2 = SNMPConfigDTO.newBuilder()
            .addAllPorts(List.of(1161))
            .addAllReadCommunity(List.of("test")).build();
        IcmpActiveDiscoveryCreateDTO request2 = IcmpActiveDiscoveryCreateDTO.newBuilder()
            .setName("test-config2")
            .setLocation("test-location2")
            .setSnmpConf(snmpConfig2)
            .addAllIpAddresses(List.of("192.168.0.1")).build();
        var result2 = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createDiscovery(request2);
        assertThat(result2).isNotNull();
    }

    @Test
    void testGetConfigById() {
        IcmpActiveDiscoveryDTO tempConfig = IcmpActiveDiscoveryDTO.newBuilder()
            .setName(TEST_NAME)
            .addAllIpAddresses(List.of("127.0.0.1"))
            .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community")).build()).build();
        var model = mapper.dtoToModel(tempConfig);
        model.setTenantId(tenantId);
        model.setCreateTime(LocalDateTime.now());
        var activeDiscovery = repository.save(model);
        IcmpActiveDiscoveryDTO discoveryConfig = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getDiscoveryById(Int64Value.of(activeDiscovery.getId()));

        assertThat(discoveryConfig).isNotNull()
            .extracting(IcmpActiveDiscoveryDTO::getName, c -> c.getIpAddressesList().get(0), c -> c.getSnmpConf().getReadCommunityList().get(0))
            .containsExactly(TEST_NAME, "127.0.0.1", "test-community");
    }

    @Test
    void testListConfig() {
        IcmpActiveDiscoveryDTO tempConfig = IcmpActiveDiscoveryDTO.newBuilder()
            .setName(TEST_NAME)
            .addAllIpAddresses(List.of("127.0.0.1"))
            .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community")).build()).build();

        IcmpActiveDiscoveryDTO tempConfig2 = IcmpActiveDiscoveryDTO.newBuilder()
            .setName("new-config")
            .addAllIpAddresses(List.of("127.0.0.2"))
            .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community2")).build()).build();
        var config1 = mapper.dtoToModel(tempConfig);
        var config2 = mapper.dtoToModel(tempConfig2);
        config1.setTenantId(tenantId);
        config1.setCreateTime(LocalDateTime.now());
        config2.setTenantId(tenantId);
        config2.setCreateTime(LocalDateTime.now());
        repository.saveAll(List.of(config1, config2));

        IcmpActiveDiscoveryList result = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listDiscoveries(Empty.getDefaultInstance());

        assertThat(result).isNotNull()
            .extracting(IcmpActiveDiscoveryList::getDiscoveriesList).asList().hasSize(2)
            .extracting("name")
            .contains(TEST_NAME, "new-config");
    }

}
