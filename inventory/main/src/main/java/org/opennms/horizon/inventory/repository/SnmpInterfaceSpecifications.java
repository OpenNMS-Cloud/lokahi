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

import jakarta.persistence.criteria.Predicate;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.service.Constants;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SnmpInterfaceSpecifications {

    public static Specification<SnmpInterface> buildSpecification(String search, Long nodeId, String tenantId) {
        return (root, query, criteriaBuilder) -> {
            Predicate conjunction = criteriaBuilder.conjunction();
            Predicate disjunction = criteriaBuilder.disjunction();

            if (search != null) {

                Predicate likeName =
                        criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_NAME), "%" + search + "%");
                Predicate likeDesc =
                        criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_DESCR), "%" + search + "%");
                Predicate likePhyAddr =
                        criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_PHY_ADDR), "%" + search + "%");
                Predicate likeAlias =
                        criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_ALIAS), "%" + search + "%");
                disjunction = criteriaBuilder.or(likeName, likeDesc, likePhyAddr, likeAlias);
            }
            Predicate tenant = null;
            if (tenantId != null) {
                tenant = criteriaBuilder.equal(root.get(Constants.SNMP_INTERFACE_COL_TENANT_ID), tenantId);
            }
            Predicate node = null;
            if (nodeId != null) {
                node = criteriaBuilder.equal(root.get("node").get(Constants.SNMP_INTERFACE_COL_NODE_ID), nodeId);
            }
            conjunction = criteriaBuilder.and(tenant, node);
            return criteriaBuilder.and(conjunction, disjunction);
        };
    }
}
