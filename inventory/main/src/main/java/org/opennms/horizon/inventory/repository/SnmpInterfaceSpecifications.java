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
package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.service.Constants;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SnmpInterfaceSpecifications {

    public static Specification<SnmpInterface> hasName(String search) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_NAME), "%" + search + "%");
    }

    public static Specification<SnmpInterface> hasDesc(String search) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_DESCR), "%" + search + "%");
    }

    public static Specification<SnmpInterface> hasAlias(String search) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_ALIAS), "%" + search + "%");
    }

    public static Specification<SnmpInterface> hasPhysicalAddress(String search) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_PHY_ADDR), "%" + search + "%");
    }

    public static Specification<SnmpInterface> hasNodeId(String search) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("node").get(Constants.SNMP_INTERFACE_COL_NODE_ID), Long.valueOf(search));
    }
}
