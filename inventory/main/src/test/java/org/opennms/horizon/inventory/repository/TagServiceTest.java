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

package org.opennms.horizon.inventory.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.model.MonitoringPolicyTag;
import org.opennms.horizon.inventory.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@AutoConfigureObservability
// Make sure to include Metrics (for some reason they are disabled by default in the integration grey-box test)
public class TagServiceTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private MonitoringPolicyTagRepository monitoringPolicyTagRepository;


    @Test
    public void testTagCreate() {

        var tagCreate = TagCreateDTO.newBuilder().setName("tag1").build();
        var tagList = TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder().setMonitoringPolicyId(1L).build())
            .addTags(tagCreate).build();
        tagService.addTags("tenant1", tagList);

        var list =  tagService.getTags("tenant1", ListAllTagsParamsDTO.newBuilder()
            .setParams(TagListParamsDTO.newBuilder().setSearchTerm("tag1").build()).build());
        Assertions.assertEquals(1, list.size());
        var tagDTO = list.get(0);
        var policyTag = new MonitoringPolicyTag();
        policyTag.setTagId(tagDTO.getId());
        policyTag.setMonitoringPolicyId(1L);
        var policyTagReturned = monitoringPolicyTagRepository.findById(policyTag);
        Assertions.assertTrue(policyTagReturned.isPresent());

    }
}
