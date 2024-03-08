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

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TagPublisherTest {
    @Mock
    private TagRepository tagRepository;

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @InjectMocks
    private TagPublisher tagPublisher;

    private List<Tag> testTagList;
    private List<Tag> testFilteredTagList;

    @BeforeEach
    void beforeTest() {

        ReflectionTestUtils.setField(tagPublisher, "tagTopic", "test-topic");
    }

    @Test
    void testPublishAllTags() {
        setupTestTagList();

        when(tagRepository.findAll()).thenReturn(testTagList);
        tagPublisher.publishAllTags();

        var matcher = prepareTagOperationKafkaMessageMatcher(
                (tagOperationProto) -> tagListMatchesTagOperationList(testFilteredTagList, tagOperationProto));

        verify(kafkaTemplate).send(Mockito.argThat(matcher));
    }

    @Test
    void testPublishTagUpdate() {
        //
        // Setup Test Data and Interactions
        //
        List<TagOperationProto> opList = List.of(
                TagOperationProto.newBuilder().setTagName("x-tag-name1-x").build(),
                TagOperationProto.newBuilder().setTagName("x-tag-name2-x").build());

        //
        // Execute
        //
        tagPublisher.publishTagUpdate(opList);

        //
        // Verify the Results
        //
        var matcher = prepareTagOperationKafkaMessageMatcher((actualList) -> Objects.equals(opList, actualList));
        verify(kafkaTemplate, timeout(3000)).send(Mockito.argThat(matcher));
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void setupTestTagList() {
        Tag t1 = mock(Tag.class);
        Tag t2 = mock(Tag.class);
        when(t2.getNodes()).thenReturn(List.of(mock(Node.class)));
        when(t2.getName()).thenReturn("FRED");
        when(t2.getTenantId()).thenReturn("TENANT");

        testTagList = List.of(t1, t2);
        testFilteredTagList = List.of(t2);
    }

    private ArgumentMatcher<ProducerRecord<String, byte[]>> prepareTagOperationKafkaMessageMatcher(
            Predicate<List<TagOperationProto>> tagOperationListMatcher) {
        return (argument) -> tagOperationKafkaMessageMatches(argument, tagOperationListMatcher);
    }

    private boolean tagOperationKafkaMessageMatches(
            ProducerRecord<String, byte[]> producerRecord, Predicate<List<TagOperationProto>> tagOperationListMatcher) {
        try {
            byte[] payload = producerRecord.value();
            TagOperationList tagOperationList = TagOperationList.parseFrom(payload);

            List<TagOperationProto> tagOperationLists = tagOperationList.getTagsList();

            return tagOperationListMatcher.test(tagOperationLists);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private boolean tagListMatchesTagOperationList(List<Tag> tagList, List<TagOperationProto> tagOperationProtoList) {
        if (tagList.size() != tagOperationProtoList.size()) {
            return false;
        }

        int cur = 0;
        while (cur < tagList.size()) {
            var tag = tagList.get(cur);
            var tagOperation = tagOperationProtoList.get(cur);

            if (!tagMatchesTagOperation(tag, tagOperation)) {
                return false;
            }

            cur++;
        }

        return true;
    }

    private boolean tagMatchesTagOperation(Tag expectedTag, TagOperationProto actualTag) {
        return ((Objects.equals(expectedTag.getName(), actualTag.getTagName()))
                && (Objects.equals(expectedTag.getTenantId(), actualTag.getTenantId()))
                && tagNodeIdsMatch(expectedTag.getNodes(), actualTag.getNodeIdList()));
    }

    private boolean tagNodeIdsMatch(List<Node> expectedNodeList, List<Long> actualNodeIdList) {
        if (expectedNodeList.size() != actualNodeIdList.size()) {
            return false;
        }

        int cur = 0;
        while (cur < expectedNodeList.size()) {
            var expectedId = expectedNodeList.get(cur).getId();
            if (expectedId != actualNodeIdList.get(cur)) {
                return false;
            }

            cur++;
        }

        return true;
    }
}
