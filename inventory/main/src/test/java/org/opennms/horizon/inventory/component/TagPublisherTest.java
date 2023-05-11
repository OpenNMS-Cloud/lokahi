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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagPublisherTest {
    @Mock
    private TagRepository tagRepository;

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @InjectMocks
    TagPublisher tagPublisher;

    @BeforeEach
    void beforeTest() {
        ReflectionTestUtils.setField(tagPublisher, "tagTopic", "test-topic");
    }

    @Test
    void testPublishAllTags() {
        List<Tag> tags = getTags();
        when(tagRepository.findAll()).thenReturn(tags);
        tagPublisher.publishAllTags();
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    private List<Tag> getTags() {
        List<Tag> tags = new ArrayList<>();
        Tag t1 = mock(Tag.class);
        Tag t2 = mock(Tag.class);
        when(t2.getNodes()).thenReturn(Arrays.asList(mock(Node.class)));
        when(t2.getName()).thenReturn("FRED");
        when(t2.getTenantId()).thenReturn("TENANT");
        tags.add(t1);
        tags.add(t2);
        return tags;
    }
}
