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

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.inventory.dto.NodeDTO;

@RequiredArgsConstructor
public class NodeTestSteps {
    private final KafkaTestHelper kafkaTestHelper;
    private final BackgroundSteps background;
    private final TenantSteps tenantSteps;
    private String nodeTopic;
    private final List<NodeDTO.Builder> builders = new ArrayList<>();

    @Given("Kafka node topic {string}")
    public void kafkaTagTopic(String nodeTopic) {
        this.nodeTopic = nodeTopic;
        kafkaTestHelper.setKafkaBootstrapUrl(background.getKafkaBootstrapUrl());
        kafkaTestHelper.startConsumerAndProducer(nodeTopic, nodeTopic);
    }

    @Given("[Node] operation data")
    public void nodeData(DataTable data) {
        for (Map<String, String> map : data.asMaps()) {
            NodeDTO.Builder builder = NodeDTO.newBuilder();
            builder.setNodeLabel(map.get("label"))
                    .setId(Long.parseLong(map.get("id")))
                    .setTenantId(map.get("tenant_id"));

            builders.add(builder);
        }
    }

    @And("Sent node message to Kafka topic")
    public void sentMessageToKafkaTopic() {
        for (NodeDTO.Builder builder : builders) {
            NodeDTO node = builder.build();
            kafkaTestHelper.sendToTopic(nodeTopic, node.toByteArray(), tenantSteps.getTenantId());
        }
    }
}
