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
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
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
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PassiveDiscoveryTaggingStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;

    private PassiveDiscoveryDTO passiveDiscovery1;
    private PassiveDiscoveryDTO passiveDiscovery2;
    private TagListDTO addedTagList;
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

    @Given("[PassiveDiscovery] Grpc location {string}")
    public void grpcLocation(String location) {
        backgroundHelper.grpcLocation(location);
    }

    @Given("[PassiveDiscovery] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    /*
     * SCENARIO GIVEN
     * *********************************************************************************
     */
    @Given("A new passive discovery")
    public void aNewPassiveDiscovery() {
        deleteAllTags();
        deleteAllPassiveDiscovery();

        var passiveDiscoveryServiceBlockingStub = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        passiveDiscovery1 = passiveDiscoveryServiceBlockingStub.upsertDiscovery(PassiveDiscoveryUpsertDTO.newBuilder()
            .setName("discovery-name").setLocation("location1").addCommunities("public").addPorts(161).build());
    }

    @Given("2 new passive discovery")
    public void twoNewPassiveDiscovery() {
        deleteAllTags();
        deleteAllPassiveDiscovery();

        var passiveDiscoveryServiceBlockingStub = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        passiveDiscovery1 = passiveDiscoveryServiceBlockingStub.upsertDiscovery(PassiveDiscoveryUpsertDTO.newBuilder()
            .setName("discovery-name").setLocation("location1").addCommunities("public").addPorts(161).build());
        passiveDiscovery2 = passiveDiscoveryServiceBlockingStub.upsertDiscovery(PassiveDiscoveryUpsertDTO.newBuilder()
            .setName("discovery-name").setLocation("location2").addCommunities("public").addPorts(161).build());
    }

    @Given("A new passive discovery with tags {string}")
    public void aNewPassiveDiscoveryWithTags(String tags) {
        deleteAllTags();
        deleteAllPassiveDiscovery();

        var passiveDiscoveryServiceBlockingStub = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        passiveDiscovery1 = passiveDiscoveryServiceBlockingStub.upsertDiscovery(PassiveDiscoveryUpsertDTO.newBuilder()
            .setName("discovery-name").setLocation("location1").addCommunities("public").addPorts(161).build());
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);
        addedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(passiveDiscovery1.getId())).build());
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A GRPC request to create tags {string} for passive discovery")
    public void aGRPCRequestToCreateTagsForPassiveDiscovery(String tags) {
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);
        fetchedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(passiveDiscovery1.getId())).build());
    }

    @When("A GRPC request to create tags {string} for both passive discovery")
    public void aGRPCRequestToCreateTagsForBothPassiveDiscovery(String tags) {
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);

        List<TagEntityIdDTO> tagEntityList = new ArrayList<>();
        tagEntityList.add(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscovery1.getId()).build());
        tagEntityList.add(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscovery2.getId()).build());

        fetchedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addAllEntityIds(tagEntityList).build());
    }

    @When("A GRPC request to fetch tags for passive discovery")
    public void aGRPCRequestToFetchTagsForPassiveDiscovery() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(passiveDiscovery1.getId()))
            .setParams(TagListParamsDTO.newBuilder().build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    @When("A GRPC request to remove tag {string} for passive discovery")
    public void aGRPCRequestToRemoveTagForPassiveDiscovery(String tag) {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        for (TagDTO tagDTO : addedTagList.getTagsList()) {
            if (tagDTO.getName().equals(tag)) {
                tagServiceBlockingStub.removeTags(TagRemoveListDTO.newBuilder()
                    .addAllTagIds(Collections.singletonList(Int64Value.newBuilder()
                        .setValue(tagDTO.getId()).build()))
                    .addEntityIds(TagEntityIdDTO.newBuilder()
                        .setPassiveDiscoveryId(passiveDiscovery1.getId())).build());
                break;
            }
        }
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(passiveDiscovery1.getId()))
            .setParams(TagListParamsDTO.newBuilder().build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    @When("A GRPC request to fetch all tags for passive discovery with name like {string}")
    public void aGRPCRequestToFetchAllTagsForPassiveDiscoveryWithNameLike(String searchTerm) {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(passiveDiscovery1.getId()))
            .setParams(TagListParamsDTO.newBuilder().setSearchTerm(searchTerm).build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */

    @Then("The passive discovery tag response should contain only tags {string}")
    public void thePassiveDiscoveryTagResponseShouldContainOnlyTags(String tags) {
        String[] tagArray = tags.split(",");

        assertNotNull(fetchedTagList);
        assertEquals(tagArray.length, fetchedTagList.getTagsCount());

        List<String> tagArraySorted = Arrays.stream(tagArray).sorted().toList();
        List<TagDTO> fetchedTagListSorted = fetchedTagList.getTagsList().stream()
            .sorted(Comparator.comparing(TagDTO::getName)).toList();

        for (int index = 0; index < tagArraySorted.size(); index++) {
            assertEquals(tagArraySorted.get(index), fetchedTagListSorted.get(index).getName());
        }
    }

    @Then("The passive discovery tag response should contain an empty list of tags")
    public void thePassiveDiscoveryTagResponseShouldContainAnEmptyListOfTags() {
        assertNotNull(fetchedTagList);
        assertEquals(0, fetchedTagList.getTagsCount());
    }

    @And("Both passive discovery have the same tags of {string}")
    public void bothPassiveDiscoveryHaveTheSameTagsOf(String tags) {
        String[] tagArray = tags.split(",");

        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        TagListDTO discovery1TagList = tagServiceBlockingStub.getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscovery1.getId())).build());
        TagListDTO discovery2TagList = tagServiceBlockingStub.getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscovery2.getId())).build());

        assertEquals(tagArray.length, discovery1TagList.getTagsCount());
        assertEquals(discovery1TagList.getTagsCount(), discovery2TagList.getTagsCount());

        List<String> tagArraySorted = Arrays.stream(tagArray).sorted().toList();
        List<TagDTO> discovery1TagListSorted = discovery1TagList.getTagsList().stream()
            .sorted(Comparator.comparing(TagDTO::getName)).toList();
        List<TagDTO> discovery2TagListSorted = discovery2TagList.getTagsList().stream()
            .sorted(Comparator.comparing(TagDTO::getName)).toList();

        assertEquals(discovery1TagListSorted, discovery2TagListSorted);

        for (int index = 0; index < tagArraySorted.size(); index++) {
            assertEquals(tagArraySorted.get(index), discovery1TagListSorted.get(index).getName());
        }
    }

    /*
     * INTERNAL
     * *********************************************************************************
     */
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

    private static List<TagCreateDTO> getTagCreateList(String[] tagArray) {
        List<TagCreateDTO> tagCreateList = new ArrayList<>();
        for (String name : tagArray) {
            tagCreateList.add(TagCreateDTO.newBuilder().setName(name).build());
        }
        return tagCreateList;
    }
}
