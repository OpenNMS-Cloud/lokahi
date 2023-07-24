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

package org.opennms.horizon.inventory.cucumber.steps.tags;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO.Builder;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PassiveDiscoveryTaggingStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;

    private Builder passiveDiscoveryDTO;
    private TagListDTO fetchedTagList;

    public PassiveDiscoveryTaggingStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

    /*
     * BACKGROUND GIVEN
     * *********************************************************************************
     */
    @Given("[PassiveDiscovery] External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[PassiveDiscovery] Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[PassiveDiscovery] Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[PassiveDiscovery] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    /*
     * SCENARIO GIVEN
     * *********************************************************************************
     */
    @Given("[PassiveDiscovery] A clean system")
    public void aCleanSystem() {
        deleteAllTags();
        deleteAllPassiveDiscovery();
    }
    
    @Given("Passive discovery communities {string}")
    public void passiveDiscoveryCommunities(String communities) {
        initializePassiveDiscoveryDto().addAllCommunities(Arrays.stream(communities.split(","))
            .map(String::trim)
            .toList()
        );
    }

    @Given("Passive discovery ports {string}")
    public void passiveDiscoveryPorts(String ports) {
        initializePassiveDiscoveryDto().addAllPorts(Arrays.stream(ports.split(","))
            .map(String::trim)
            .map(Integer::parseInt)
            .toList()
        );
    }

    @Given("Passive discovery tags {string}")
    public void passiveDiscoveryTags(String tags) {
        initializePassiveDiscoveryDto().addAllTags(Arrays.stream(tags.split(","))
            .map(String::trim)
            .filter(str -> !str.isEmpty())
            .map(name -> TagCreateDTO.newBuilder().setName(name).build())
            .toList()
        );
    }

    @Given("Passive discovery location named {string}")
    public void passiveDiscoveryLocation(String location) {
        initializePassiveDiscoveryDto().setLocationId(backgroundHelper.findLocationId(location));
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A new passive discovery named {string} is created")
    public void aNewPassiveDiscovery(String label) {
        try {
            var passiveDiscoveryServiceBlockingStub = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
            passiveDiscoveryServiceBlockingStub.upsertDiscovery(initializePassiveDiscoveryDto()
                .setName(label)
                .build()
            );
        } finally {
            // reset scenario state
            this.passiveDiscoveryDTO = null;
        }
    }

    @When("A GRPC request to create tags {string} for passive discovery with label {string}")
    public void aGRPCRequestToCreateTagsForPassiveDiscovery(String tags, String discoveryLabel) {
        String[] tagArray = tags.split(",");
        PassiveDiscoveryDTO passiveDiscovery = findPassiveDiscovery(discoveryLabel);
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);

        tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addEntityIds(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscovery.getId()))
            .build()
        );
    }

    @When("A GRPC request to create tags {string} for both passive discovery with label {string}")
    public void aGRPCRequestToCreateTagsForBothPassiveDiscovery(String tags, String label) {
        String[] tagArray = tags.split(",");
        PassiveDiscoveryDTO passiveDiscovery = findPassiveDiscovery(
            label);
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);

        List<TagEntityIdDTO> tagEntityList = new ArrayList<>();
        tagEntityList.add(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscovery.getId()).build());

        fetchedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addAllEntityIds(tagEntityList).build());
    }

    @When("A GRPC request to fetch tags for passive discovery with label {string}")
    public void aGRPCRequestToFetchTagsForPassiveDiscovery(String label) {
        PassiveDiscoveryDTO passiveDiscovery = findPassiveDiscovery(label);
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(passiveDiscovery.getId()))
            .setParams(TagListParamsDTO.newBuilder().build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    @When("A GRPC request to remove tag {string} for passive discovery with label {string}")
    public void aGRPCRequestToRemoveTagForPassiveDiscovery(String tag, String label) {
        PassiveDiscoveryDTO passiveDiscovery = findPassiveDiscovery(label);
        fetchPassiveDiscoveryTags(label);

        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        for (TagDTO tagDTO : fetchedTagList.getTagsList()) {
            if (tagDTO.getName().equals(tag)) {
                tagServiceBlockingStub.removeTags(TagRemoveListDTO.newBuilder()
                    .addAllTagIds(Collections.singletonList(Int64Value.newBuilder()
                        .setValue(tagDTO.getId()).build()))
                    .addEntityIds(TagEntityIdDTO.newBuilder()
                        .setPassiveDiscoveryId(passiveDiscovery.getId())).build());
                break;
            }
        }
    }

    @When("A GRPC request to fetch passive discovery {string} tags with name like {string}")
    public void aGRPCRequestToFetchAllTagsForPassiveDiscoveryWithNameLike(String label, String searchTerm) {
        PassiveDiscoveryDTO passiveDiscovery = findPassiveDiscovery(label);
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(passiveDiscovery.getId()))
            .setParams(TagListParamsDTO.newBuilder().setSearchTerm(searchTerm).build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */

    @Then("Fetch tags for passive discovery {string}")
    public void fetchPassiveDiscoveryTags(String label) {
        PassiveDiscoveryDTO passiveDiscovery = findPassiveDiscovery(label);

        this.fetchedTagList = backgroundHelper.getTagServiceBlockingStub().getTagsByEntityId(
            ListTagsByEntityIdParamsDTO.newBuilder()
                .setEntityId(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscovery.getId()))
                .build()
        );
    }

    @Then("The passive discovery tag response should contain only tags {string}")
    public void thePassiveDiscoveryTagResponseShouldContainOnlyTags(String tags) {
        String[] tagArray = tags.split(",");

        assertNotNull(fetchedTagList);

        Set<String> givenTags = Arrays.stream(tagArray)
            .collect(Collectors.toSet());
        Set<String> fetchedTags = fetchedTagList.getTagsList().stream()
            .map(TagDTO::getName)
            .collect(Collectors.toSet());

        assertEquals(givenTags, fetchedTags);
    }

    @Then("The passive discovery tag response should contain an empty list of tags")
    public void thePassiveDiscoveryTagResponseShouldContainAnEmptyListOfTags() {
        assertNotNull(fetchedTagList);
        assertEquals(0, fetchedTagList.getTagsCount());
    }

    /*
     * INTERNAL
     * *********************************************************************************
     */
    private Builder initializePassiveDiscoveryDto() {
        if (this.passiveDiscoveryDTO == null) {
            this.passiveDiscoveryDTO = PassiveDiscoveryUpsertDTO.newBuilder();
        }
        return passiveDiscoveryDTO;
    }

    private void deleteAllTags() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<Int64Value> tagIds = tagServiceBlockingStub.getTags(ListAllTagsParamsDTO.newBuilder().build())
            .getTagsList().stream().map(tagDTO -> Int64Value.of(tagDTO.getId())).toList();
        tagServiceBlockingStub.deleteTags(DeleteTagsDTO.newBuilder().addAllTagIds(tagIds).build());
    }

    private void deleteAllPassiveDiscovery() {
        var passiveDiscoveryServiceBlockingStub = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        for (PassiveDiscoveryDTO discoveryDTO : passiveDiscoveryServiceBlockingStub.listAllDiscoveries(Empty.newBuilder().build()).getDiscoveriesList()) {
            passiveDiscoveryServiceBlockingStub.deleteDiscovery(Int64Value.of(discoveryDTO.getId()));
        }
    }

    private PassiveDiscoveryDTO findPassiveDiscovery(String label) {
        return backgroundHelper.getPassiveDiscoveryServiceBlockingStub()
            .listAllDiscoveries(Empty.getDefaultInstance())
            .getDiscoveriesList().stream()
            .filter(disco -> label.equals(disco.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Passive discovery with label " + label + " not found"));
    }

    private static List<TagCreateDTO> getTagCreateList(String[] tagArray) {
        List<TagCreateDTO> tagCreateList = new ArrayList<>();
        for (String name : tagArray) {
            tagCreateList.add(TagCreateDTO.newBuilder().setName(name).build());
        }
        return tagCreateList;
    }


}
