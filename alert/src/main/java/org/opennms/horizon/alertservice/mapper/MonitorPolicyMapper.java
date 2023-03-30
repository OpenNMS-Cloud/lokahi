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

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyProto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Mapper(componentModel = "spring", uses ={PolicyRuleMapper.class},
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface MonitorPolicyMapper {
    ObjectMapper objectMapper = new ObjectMapper();
    default List<String> jsonToList(JsonNode data) {
        List<String> list = new ArrayList<>();
        if(data.isArray()) {
            ArrayNode arrayNode = (ArrayNode) data;
            arrayNode.forEach(tag -> list.add(tag.textValue()));
        }
        return list;
    }
    default JsonNode map(List<String> list) {
        return objectMapper.valueToTree(list);
    }

    default MonitorPolicyProto entityToProto(MonitorPolicy policy) {
        MonitorPolicyProto tmp = map(policy);
        return MonitorPolicyProto.newBuilder(tmp)
            .addAllTags(jsonToList(policy.getTags())).build();
    }

    @Mappings({
        @Mapping(target = "rulesList", source = "rules")
    })
    MonitorPolicyProto map(MonitorPolicy policy);

    @Mappings({
        @Mapping(target = "tags", source = "tagsList"),
        @Mapping(target = "rules", source = "rulesList")
    })
    MonitorPolicy map(MonitorPolicyProto proto);
}
