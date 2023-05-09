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

package org.opennms.horizon.inventory.grpc.discovery;

import com.google.protobuf.Empty;
import com.google.protobuf.ProtocolStringList;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryListDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.inventory.grpc.GrpcTestBase;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@AutoConfigureObservability     // Make sure to include Metrics (for some reason they are disabled by default in the integration grey-box test)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PassiveDiscoveryGrpcItTest extends GrpcTestBase {
    private static final String DEFAULT_LOCATION = "Default";
    private static final String TEST_TAG_NAME_1 = "tag-name-1";
    private static final String TEST_TAG_NAME_2 = "tag-name-2";
    private static final int TEST_SNMP_PORT = 161;
    private static final String TEST_SNMP_COMMUNITY = "public";
    private static final String TEST_ANOTHER_LOCATION = "Another Location";
    private static final int TEST_ANOTHER_PORT = 162;
    private static final String TEST_ANOTHER_COMMUNITY = "private";

    private PassiveDiscoveryServiceGrpc.PassiveDiscoveryServiceBlockingStub serviceStub;
    private TagServiceGrpc.TagServiceBlockingStub tagServiceStub;

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareTestGrpc();
        prepareServer();
        serviceStub = PassiveDiscoveryServiceGrpc.newBlockingStub(channel);
        tagServiceStub = TagServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        afterTest();
    }

    @Test
    void testCreatePassiveDiscovery() {
        TagCreateDTO tagCreateDto1 = TagCreateDTO.newBuilder().setName(TEST_TAG_NAME_1).build();

        PassiveDiscoveryUpsertDTO upsertDTO = PassiveDiscoveryUpsertDTO.newBuilder()
            .setLocation(DEFAULT_LOCATION)
            .addPorts(TEST_SNMP_PORT)
            .addCommunities(TEST_SNMP_COMMUNITY)
            .addAllTags(List.of(tagCreateDto1))
            .build();

        PassiveDiscoveryDTO passiveDiscovery = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .upsertDiscovery(upsertDTO);

        TagListDTO tagListDto = tagServiceStub.withInterceptors(MetadataUtils
            .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
                .setEntityId(TagEntityIdDTO.newBuilder()
                    .setPassiveDiscoveryId(passiveDiscovery.getId())).build());
        List<TagDTO> tagsList = tagListDto.getTagsList();
        assertEquals(upsertDTO.getTagsCount(), tagsList.size());

        TagDTO tagDTO = tagsList.get(0);
        assertEquals(upsertDTO.getTags(0).getName(), tagDTO.getName());

        assertEquals(upsertDTO.getLocation(), passiveDiscovery.getLocation());
        assertTrue(passiveDiscovery.getToggle());
        assertTrue(passiveDiscovery.getCreateTimeMsec() > 0);

        assertEquals(upsertDTO.getPortsCount(), passiveDiscovery.getPortsCount());
        List<Integer> portsList = passiveDiscovery.getPortsList();
        assertEquals(TEST_SNMP_PORT, portsList.get(0));

        assertEquals(upsertDTO.getCommunitiesCount(), passiveDiscovery.getCommunitiesCount());
        ProtocolStringList communitiesList = passiveDiscovery.getCommunitiesList();
        assertEquals(TEST_SNMP_COMMUNITY, communitiesList.get(0));

        assertTrue(passiveDiscovery.getCreateTimeMsec() > 0);
    }

    @Test
    void testUpdatePassiveDiscovery() {
        TagCreateDTO tagCreateDto1 = TagCreateDTO.newBuilder().setName(TEST_TAG_NAME_1).build();

        PassiveDiscoveryUpsertDTO upsertDTO1 = PassiveDiscoveryUpsertDTO.newBuilder()
            .setLocation(DEFAULT_LOCATION)
            .addPorts(TEST_SNMP_PORT)
            .addCommunities(TEST_SNMP_COMMUNITY)
            .addAllTags(List.of(tagCreateDto1))
            .build();

        PassiveDiscoveryDTO passiveDiscovery1 = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .upsertDiscovery(upsertDTO1);

        TagCreateDTO tagCreateDto2 = TagCreateDTO.newBuilder().setName(TEST_TAG_NAME_2).build();

        PassiveDiscoveryUpsertDTO upsertDTO2 = PassiveDiscoveryUpsertDTO.newBuilder()
            .setId(passiveDiscovery1.getId())
            .setLocation(TEST_ANOTHER_LOCATION)
            .addPorts(TEST_ANOTHER_PORT)
            .addCommunities(TEST_ANOTHER_COMMUNITY)
            .addAllTags(List.of(tagCreateDto2))
            .build();

        PassiveDiscoveryDTO passiveDiscovery2 = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .upsertDiscovery(upsertDTO2);

        TagListDTO tagListDto = tagServiceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
                .setEntityId(TagEntityIdDTO.newBuilder()
                    .setPassiveDiscoveryId(passiveDiscovery2.getId())).build());
        List<TagDTO> tagsList = tagListDto.getTagsList();
        assertEquals(upsertDTO2.getTagsCount(), tagsList.size());

        TagDTO tagDTO = tagsList.get(0);
        assertEquals(upsertDTO2.getTags(0).getName(), tagDTO.getName());

        assertEquals(upsertDTO2.getLocation(), passiveDiscovery2.getLocation());
        assertTrue(passiveDiscovery2.getToggle());
        assertTrue(passiveDiscovery2.getCreateTimeMsec() > 0);

        assertEquals(upsertDTO2.getPortsCount(), passiveDiscovery2.getPortsCount());
        List<Integer> portsList = passiveDiscovery2.getPortsList();
        assertEquals(TEST_ANOTHER_PORT, portsList.get(0));

        assertEquals(upsertDTO2.getCommunitiesCount(), passiveDiscovery2.getCommunitiesCount());
        ProtocolStringList communitiesList = passiveDiscovery2.getCommunitiesList();
        assertEquals(TEST_ANOTHER_COMMUNITY, communitiesList.get(0));

        assertTrue(passiveDiscovery2.getCreateTimeMsec() > 0);
    }

    @Test
    void testListPassiveDiscovery() {
        TagCreateDTO tagCreateDto1 = TagCreateDTO.newBuilder().setName(TEST_TAG_NAME_1).build();

        PassiveDiscoveryUpsertDTO upsertDTO = PassiveDiscoveryUpsertDTO.newBuilder()
            .setLocation(DEFAULT_LOCATION)
            .addPorts(TEST_SNMP_PORT)
            .addCommunities(TEST_SNMP_COMMUNITY)
            .addAllTags(List.of(tagCreateDto1))
            .build();

        serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .upsertDiscovery(upsertDTO);

        PassiveDiscoveryListDTO listDto = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listAllDiscoveries(Empty.getDefaultInstance());
        List<PassiveDiscoveryDTO> list = listDto.getDiscoveriesList();
        assertEquals(1, list.size());

        PassiveDiscoveryDTO passiveDiscovery = listDto.getDiscoveries(0);

        assertEquals(upsertDTO.getLocation(), passiveDiscovery.getLocation());
        assertTrue(passiveDiscovery.getToggle());
        assertTrue(passiveDiscovery.getCreateTimeMsec() > 0);

        assertEquals(upsertDTO.getPortsCount(), passiveDiscovery.getPortsCount());
        List<Integer> portsList = passiveDiscovery.getPortsList();
        assertEquals(TEST_SNMP_PORT, portsList.get(0));

        assertEquals(upsertDTO.getCommunitiesCount(), passiveDiscovery.getCommunitiesCount());
        ProtocolStringList communitiesList = passiveDiscovery.getCommunitiesList();
        assertEquals(TEST_SNMP_COMMUNITY, communitiesList.get(0));

        assertTrue(passiveDiscovery.getCreateTimeMsec() > 0);
    }

    @Test
    void testTogglePassiveDiscovery() {

        // create a passive discovery
        PassiveDiscoveryUpsertDTO upsertDTO = PassiveDiscoveryUpsertDTO.newBuilder()
            .build();
        // insert the passive discovery, should default toggle to true
        PassiveDiscoveryDTO passiveDiscovery = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .upsertDiscovery(upsertDTO);

        // toggle it to false
        PassiveDiscoveryToggleDTO toggleDTO = PassiveDiscoveryToggleDTO.newBuilder()
            .setId(passiveDiscovery.getId())
            .setToggle(false)
            .build();
        PassiveDiscoveryDTO passiveDiscovery2 = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .toggleDiscovery(toggleDTO);

        // check id and toggle
        assertEquals(passiveDiscovery.getId(), passiveDiscovery2.getId());
        assertEquals(toggleDTO.getToggle(), passiveDiscovery2.getToggle());
    }
}
