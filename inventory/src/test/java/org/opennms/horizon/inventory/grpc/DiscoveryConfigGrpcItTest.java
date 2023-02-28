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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigDTO;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigList;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigOperationGrpc;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigRequest;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.repository.ConfigurationRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.Context;
import io.grpc.stub.MetadataUtils;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class DiscoveryConfigGrpcItTest extends GrpcTestBase {
    @Autowired
    private ConfigurationRepository configRepo;
    private DiscoveryConfigOperationGrpc.DiscoveryConfigOperationBlockingStub serviceStub;
    private final String configName = "test-config";
    private final String location = "test-location";

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = DiscoveryConfigOperationGrpc.newBlockingStub(channel);
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
        DiscoveryConfigRequest request = DiscoveryConfigRequest.newBuilder()
            .setConfigName("test-config")
            .setLocation("test-location")
            .setSnmpConf(snmpConfig)
            .addAllIpAddresses(List.of("127.0.0.1-127.0.0.10")).build();

        DiscoveryConfigList result = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createConfig(request);
        assertThat(result).isNotNull()
            .extracting(DiscoveryConfigList::getDiscoverConfigsList).asList().hasSize(1);

        SNMPConfigDTO snmpConfig2 = SNMPConfigDTO.newBuilder()
            .addAllPorts(List.of(1161))
            .addAllReadCommunity(List.of("test")).build();
        DiscoveryConfigRequest request2 = DiscoveryConfigRequest.newBuilder()
            .setConfigName("test-config2")
            .setLocation("test-location2")
            .setSnmpConf(snmpConfig2)
            .addAllIpAddresses(List.of("192.168.0.1")).build();
        DiscoveryConfigList list2 = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createConfig(request2);
        assertThat(list2).isNotNull()
            .extracting(DiscoveryConfigList::getDiscoverConfigsList).asList().hasSize(2);
    }

    @Test
    void testGetConfigByName() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            DiscoveryConfigDTO tempConfig = DiscoveryConfigDTO.newBuilder()
                .setConfigName(configName)
                .addAllIpAddresses(List.of("127.0.0.1"))
                .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community")).build()).build();
            Configuration configuration = new Configuration();
            configuration.setKey(ConfigKey.DISCOVERY);
            configuration.setTenantId(tenantId);
            configuration.setLocation(location);
            configuration.setValue(listToJson(List.of(tempConfig)));
            configRepo.save(configuration);
        });

        DiscoveryConfigDTO discoveryConfig = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
                .getDiscoveryConfigByName(StringValue.of(configName));

        assertThat(discoveryConfig).isNotNull()
            .extracting(DiscoveryConfigDTO::getConfigName, c -> c.getIpAddressesList().get(0), c -> c.getSnmpConf().getReadCommunityList().get(0))
            .containsExactly(configName, "127.0.0.1", "test-community");
    }

    @Test
    void testListConfig() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            DiscoveryConfigDTO tempConfig = DiscoveryConfigDTO.newBuilder()
                .setConfigName(configName)
                .addAllIpAddresses(List.of("127.0.0.1"))
                .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community")).build()).build();

            DiscoveryConfigDTO tempConfig2 = DiscoveryConfigDTO.newBuilder()
                .setConfigName("new-config")
                .addAllIpAddresses(List.of("127.0.0.2"))
                .setSnmpConf(SNMPConfigDTO.newBuilder().addAllReadCommunity(List.of("test-community2")).build()).build();
            Configuration configuration = new Configuration();
            configuration.setKey(ConfigKey.DISCOVERY);
            configuration.setTenantId(tenantId);
            configuration.setLocation(location);
            configuration.setValue(listToJson(List.of(tempConfig, tempConfig2)));
            configRepo.save(configuration);
        });

        DiscoveryConfigList result = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listDiscoveryConfig(Empty.getDefaultInstance());

        assertThat(result).isNotNull()
            .extracting(DiscoveryConfigList::getDiscoverConfigsList).asList().hasSize(2)
            .extracting("configName")
            .contains(configName, "new-config");
    }


    private JsonNode listToJson(List<DiscoveryConfigDTO> list) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        list.forEach(c -> {
            try {
                arrayNode.add(objectMapper.readTree(ProtobufUtil.toJson(c)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return arrayNode;
    }
}
