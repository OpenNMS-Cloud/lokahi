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
package org.opennms.horizon.inventory.component;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagPublishConsumer {

    private final TagService tagService;

    @KafkaListener(topics = "${kafka.topics.tag-operation}", concurrency = "${kafka.concurrency.tag-operation}")
    public void receiveMessage(@Payload byte[] data) {

        try {
            TagOperationList operationList = TagOperationList.parseFrom(data);
            tagService.insertOrUpdateTags(operationList);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing TagOperationList, payload data {}", Arrays.toString(data), e);
        }
    }
}
