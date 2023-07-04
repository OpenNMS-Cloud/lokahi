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

package org.opennms.horizon.alertservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.Tag;

@Mapper(componentModel = "spring", uses ={PolicyRuleMapper.class},
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface MonitorPolicyMapper {
    @Mappings({
        @Mapping(target = "rulesList", source = "rules"),
        @Mapping(target = "tagsList", source = "tags")
    })
    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    MonitorPolicyProto map(MonitorPolicy policy);

    @Mappings({
        @Mapping(target = "rules", source = "rulesList"),
        @Mapping(target = "tags", source = "tagsList")
    })
    MonitorPolicy map(MonitorPolicyProto proto);

    default String map(Tag tag) {
        if(tag != null) {
            return tag.getName();
        }
        return null;
    }

    default Tag map(String tagName) {
        var tag = new Tag();
        tag.setName(tagName);
        return tag;

    }
}
