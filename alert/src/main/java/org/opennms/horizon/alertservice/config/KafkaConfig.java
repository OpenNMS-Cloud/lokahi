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
        return getTopicBuilder(kafkaTopicProperties.getCreateTopics().getAlert()).build();
    }

    @Bean
    public NewTopic monitoringPolicyTopic(KafkaTopicProperties kafkaTopicProperties) {
        return getTopicBuilder(kafkaTopicProperties.getCreateTopics().getMonitoringPolicy()).build();
    }

    @Bean
    public NewTopic nodeChangedTopic(KafkaTopicProperties kafkaTopicProperties) {
        return getTopicBuilder(kafkaTopicProperties.getCreateTopics().getNodeChanged()).build();
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
