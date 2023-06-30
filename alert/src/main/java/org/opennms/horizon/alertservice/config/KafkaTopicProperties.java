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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {

    private String trapEvent;

    private String tagOperation;

    private String alert;

    private String nodeChanged;

    private String monitoringPolicy;

    private final CreateTopics createTopics = new CreateTopics();

    @Data
    public static class CreateTopics {
        private Boolean enabled;
        private final TopicConfig alert = new TopicConfig();
        private final TopicConfig monitoringPolicy = new TopicConfig();
        private final TopicConfig nodeChanged = new TopicConfig();
    }

    @Data
    public static class TopicConfig {
        private String name;
        private Integer partitions = 10;
        private Short replicas = 1;
        private Boolean compact = false;
    }
}
