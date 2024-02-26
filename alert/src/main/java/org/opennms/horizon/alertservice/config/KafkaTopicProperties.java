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

    private String internalEvent;

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
