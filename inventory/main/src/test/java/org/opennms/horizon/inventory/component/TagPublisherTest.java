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

package org.opennms.horizon.inventory.component;

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

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.mockito.Mockito.*;

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

        var matcher = prepareTagOperationKafkaMessageMatcher((tagOperationProto) -> tagListMatchesTagOperationList(testFilteredTagList, tagOperationProto));

        verify(kafkaTemplate).send(Mockito.argThat(matcher));
    }

    @Test
    void testPublishTagUpdate() {
        //
        // Setup Test Data and Interactions
        //
        List<TagOperationProto> opList =
            List.of(
                TagOperationProto.newBuilder().setTagName("x-tag-name1-x").build(),
                TagOperationProto.newBuilder().setTagName("x-tag-name2-x").build()
            );

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

//========================================
// Internals
//----------------------------------------

    private void setupTestTagList() {
        Tag t1 = mock(Tag.class);
        Tag t2 = mock(Tag.class);
        when(t2.getNodes()).thenReturn(List.of(mock(Node.class)));
        when(t2.getName()).thenReturn("FRED");
        when(t2.getTenantId()).thenReturn("TENANT");

        testTagList = List.of(t1, t2);
        testFilteredTagList = List.of(t2);
    }

    private ArgumentMatcher<ProducerRecord<String, byte[]>> prepareTagOperationKafkaMessageMatcher(Predicate<List<TagOperationProto>> tagOperationListMatcher) {
        return (argument) -> tagOperationKafkaMessageMatches(argument, tagOperationListMatcher);
    }

    private boolean tagOperationKafkaMessageMatches(ProducerRecord<String, byte[]> producerRecord, Predicate<List<TagOperationProto>> tagOperationListMatcher) {
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

            if (! tagMatchesTagOperation(tag, tagOperation)) {
                return false;
            }

            cur++;
        }

        return true;
    }

    private boolean tagMatchesTagOperation(Tag expectedTag, TagOperationProto actualTag) {
        return (
            (Objects.equals(expectedTag.getName(), actualTag.getTagName())) &&
            (Objects.equals(expectedTag.getTenantId(), actualTag.getTenantId())) &&
            tagNodeIdsMatch(expectedTag.getNodes(), actualTag.getNodeIdList())
        );
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
