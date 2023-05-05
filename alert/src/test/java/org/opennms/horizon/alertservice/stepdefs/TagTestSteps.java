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

package org.opennms.horizon.alertservice.stepdefs;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.opennms.horizon.alert.tag.proto.TagListProto;
import org.opennms.horizon.alert.tag.proto.TagProto;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;
import org.opennms.horizon.alertservice.RetryUtils;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;

import com.google.protobuf.MessageOrBuilder;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Transpose;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TagTestSteps {
    private static final long TIME_OUT = 5000;
    private final KafkaTestHelper kafkaTestHelper;
    private final BackgroundSteps background;
    private final AlertGrpcClientUtils grpcClient;
    private final RetryUtils retryUtils;

    private String tagTopic;
    private String tenantId;
    private TagOperationProto.Builder builder;


    @Given("Kafka tag topic {string}")
    public void kafkaTagTopic(String tagTopic) {
        this.tagTopic = tagTopic;
        kafkaTestHelper.setKafkaBootstrapUrl(background.getKafkaBootstrapUrl());
        kafkaTestHelper.startConsumerAndProducer(tagTopic, tagTopic);
    }

    @Given("Tenant {string}")
    public void tenant(String tenantId) {
        this.tenantId = tenantId;
        grpcClient.setTenantId(tenantId);

    }

    @Given("Tag operation data")
    public void tagOperationData(DataTable data) {
        Map<String, String> map = data.asMaps().get(0);
        List<Long> nodIds = Arrays.stream(map.get("node_ids").split(",")).map(s -> Long.parseLong(s)).collect(Collectors.toList());
        builder = TagOperationProto.newBuilder();
        builder.setOperation(Operation.valueOf(map.get("action")))
            .setTagName(map.get("name"))
            .setTenantId(tenantId)
            .addAllNodeId(nodIds);
    }

    @And("Sent tag operation message to Kafka topic")
    public void sentMessageToKafkaTopic() {
        TagOperationList tagList = TagOperationList.newBuilder()
            .addTags(builder.build()).build();
        kafkaTestHelper.sendToTopic(tagTopic, tagList.toByteArray(), tenantId);
    }

    @Then("Verify list tag with size {int} and node ids")
    public void verifyListTagWithSizeAndNodeIds(int size, @Transpose DataTable data) throws InterruptedException {
        List<String> idStrList = data.asList();
        List<Long> ids = idStrList.stream().map(s -> Long.parseLong(s)).collect(Collectors.toList());
        Supplier<MessageOrBuilder> call = () -> grpcClient.getTagStub().listTags(TagListProto.newBuilder().build());
        boolean success = retryUtils.retry(() -> this.sendRequestAndVerify(call, size, builder, ids),
            result -> result,
            100, TIME_OUT, false);
        assertThat(success).isTrue();
    }

    @Then("Verify list tag is empty")
    public void verifyListTagIsEmpty() throws InterruptedException {
        Supplier<MessageOrBuilder> grpcCall = () -> grpcClient.getTagStub().listTags(TagListProto.newBuilder().build());
        boolean success = retryUtils.retry(() -> sendRequestAndVerify(grpcCall, 0, null, null),
            result -> result,
            100, TIME_OUT, false
        );
    }

    private boolean sendRequestAndVerify(Supplier<MessageOrBuilder> supplier, int listSize, TagOperationProto.Builder builder, List<Long> nodeIds) {
        try {
            var tagProtoList = (TagListProto) supplier.get();
            List<TagProto> list = tagProtoList.getTagsList();
            assertThat(list).asList().hasSize(listSize);
            if(listSize > 0) {
                assertThat(list.get(0))
                    .extracting(TagProto::getName, TagProto::getTenantId)
                    .containsExactly(builder.getTagName(), builder.getTenantId());
                assertThat(list.get(0).getNodeIdsList()).asList().hasSize(nodeIds.size()).containsAll(nodeIds);
            }
            return true;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
