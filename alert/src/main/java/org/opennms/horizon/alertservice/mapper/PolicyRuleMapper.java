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
package org.opennms.horizon.alertservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.alerts.proto.PolicyRuleProto;
import org.opennms.horizon.alertservice.db.entity.PolicyRule;

@Mapper(
        componentModel = "spring",
        uses = {AlertConditionMapper.class},
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PolicyRuleMapper {
    @Mapping(target = "snmpEventsList", source = "alertConditions")
    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    PolicyRuleProto entityToProto(PolicyRule rule);

    @Mapping(target = "alertConditions", source = "snmpEventsList")
    PolicyRule protoToEntity(PolicyRuleProto proto);
}
