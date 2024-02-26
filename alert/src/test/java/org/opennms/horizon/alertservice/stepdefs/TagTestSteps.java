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
package org.opennms.horizon.alertservice.stepdefs;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.protobuf.MessageOrBuilder;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Transpose;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alert.tag.proto.TagListProto;
import org.opennms.horizon.alert.tag.proto.TagProto;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;
import org.opennms.horizon.alertservice.RetryUtils;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;

@RequiredArgsConstructor
public class TagTestSteps {
    private static final long TIME_OUT = 5000;
    private final TenantSteps tenantSteps;
    private final KafkaTestHelper kafkaTestHelper;
    private final BackgroundSteps background;
    private final AlertGrpcClientUtils grpcClient;
    private final RetryUtils retryUtils;

    private TagOperationProto.Builder builder;

    @Given("Tag operations are applied to the tenant")
    public void tagOperationDataForTenant(DataTable data) {
        tagOperationData(data, tenantSteps.getTenantId());
        sentMessageToKafkaTopic(tenantSteps.getTenantId());
    }

    @Given("Tag operation data")
    public void tagOperationData(DataTable data) {
        tagOperationData(data, tenantSteps.getTenantId());
    }

    public void tagOperationData(DataTable data, String tenantId) {
        Map<String, String> map = data.asMaps().get(0);
        List<Long> nodIds = Arrays.stream(map.get("node_ids").split(","))
                .map(s -> Long.parseLong(s))
                .collect(Collectors.toList());
        List<Long> policyIds = map.get("policy_ids") != null
                ? Arrays.stream(map.get("policy_ids").split(","))
                        .map(s -> Long.parseLong(s))
                        .collect(Collectors.toList())
                : new ArrayList<>();
        builder = TagOperationProto.newBuilder();
        builder.setOperation(Operation.valueOf(map.get("action")))
                .setTagName(map.get("name"))
                .setTenantId(tenantId)
                .addAllNodeId(nodIds)
                .addAllMonitoringPolicyId(policyIds);
    }

    @And("Sent tag operation message to Kafka topic")
    public void sentMessageToKafkaTopic() {
        sentMessageToKafkaTopic(tenantSteps.getTenantId());
    }

    public void sentMessageToKafkaTopic(String tenantId) {
        TagOperationList tagList =
                TagOperationList.newBuilder().addTags(builder.build()).build();
        kafkaTestHelper.sendToTopic(background.getTagTopic(), tagList.toByteArray(), tenantId);
    }

    @Then("Verify list tag with size {int} and node ids")
    public void verifyListTagWithSizeAndNodeIds(int size, @Transpose DataTable data) throws InterruptedException {
        List<String> idStrList = data.asList();
        List<Long> ids = idStrList.stream().map(s -> Long.parseLong(s)).collect(Collectors.toList());
        Supplier<MessageOrBuilder> call =
                () -> grpcClient.getTagStub().listTags(TagListProto.newBuilder().build());
        boolean success = retryUtils.retry(
                () -> this.sendRequestAndVerify(call, size, builder, ids, new ArrayList<>()),
                result -> result,
                100,
                TIME_OUT,
                false);
        assertThat(success).isTrue();
    }

    @Then("Verify list tag is empty")
    public void verifyListTagIsEmpty() throws InterruptedException {
        Supplier<MessageOrBuilder> grpcCall =
                () -> grpcClient.getTagStub().listTags(TagListProto.newBuilder().build());
        boolean success = retryUtils.retry(
                () -> sendRequestAndVerify(grpcCall, 0, null, null, null), result -> result, 100, TIME_OUT, false);
    }

    private boolean sendRequestAndVerify(
            Supplier<MessageOrBuilder> supplier,
            int listSize,
            TagOperationProto.Builder builder,
            List<Long> nodeIds,
            List<Long> policyIds) {
        try {
            var tagProtoList = (TagListProto) supplier.get();
            List<TagProto> list = tagProtoList.getTagsList();
            assertThat(list).asList().hasSize(listSize);
            if (listSize > 0) {
                assertThat(list.get(0))
                        .extracting(TagProto::getName, TagProto::getTenantId)
                        .containsExactly(builder.getTagName(), builder.getTenantId());
                if (!nodeIds.isEmpty()) {
                    assertThat(list.get(0).getNodeIdsList())
                            .asList()
                            .hasSize(nodeIds.size())
                            .containsAll(nodeIds);
                }
                // TODO: Map policyIds list
            }
            return true;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Then("Verify list tag with size {int} and policy ids")
    public void verifyListTagWithSizeAndPolicyIds(int size, @Transpose DataTable data) throws InterruptedException {

        List<String> idStrList = data.asList();
        List<Long> ids = idStrList.stream().map(s -> Long.parseLong(s)).collect(Collectors.toList());
        Supplier<MessageOrBuilder> call =
                () -> grpcClient.getTagStub().listTags(TagListProto.newBuilder().build());
        boolean success = retryUtils.retry(
                () -> this.sendRequestAndVerify(call, size, builder, new ArrayList<>(), ids),
                result -> result,
                100,
                TIME_OUT,
                false);
        assertThat(success).isTrue();
    }
}
