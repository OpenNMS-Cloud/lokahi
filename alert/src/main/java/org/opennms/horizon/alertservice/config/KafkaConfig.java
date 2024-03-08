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
package org.opennms.horizon.alertservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "kafka.topics.create-topics", name = "enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    public NewTopic alertTopic(KafkaTopicProperties kafkaTopicProperties) {
        return getTopicBuilder(kafkaTopicProperties.getCreateTopics().getAlert())
                .build();
    }

    @Bean
    public NewTopic monitoringPolicyTopic(KafkaTopicProperties kafkaTopicProperties) {
        return getTopicBuilder(kafkaTopicProperties.getCreateTopics().getMonitoringPolicy())
                .build();
    }

    @Bean
    public NewTopic nodeChangedTopic(KafkaTopicProperties kafkaTopicProperties) {
        return getTopicBuilder(kafkaTopicProperties.getCreateTopics().getNodeChanged())
                .build();
    }

    private TopicBuilder getTopicBuilder(KafkaTopicProperties.TopicConfig topic) {
        TopicBuilder builder = TopicBuilder.name(topic.getName())
                .partitions(topic.getPartitions())
                .replicas(topic.getReplicas());

        if (topic.getCompact()) {
            builder.compact();
        }

        return builder;
    }
}
