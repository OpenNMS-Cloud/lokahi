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
package org.opennms.horizon.alertservice.db.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Getter
@Setter
@Entity
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "name")
    private String name;

    @Column(name = "node_ids", columnDefinition = "bigint[]")
    @Type(ListArrayType.class)
    private List<Long> nodeIds = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "policy_tag",
            joinColumns = @JoinColumn(name = "tag_id"),
            inverseJoinColumns = @JoinColumn(name = "policy_id"))
    private Set<MonitorPolicy> policies = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(id, name, tenantId, nodeIds, policies);
    }

    @Override
    public boolean equals(Object in) {
        if (in instanceof Tag that) {
            return Objects.equals(this.getName(), that.getName())
                    && Objects.equals(this.getId(), that.getId())
                    && Objects.equals(this.getTenantId(), that.getTenantId())
                    && Objects.equals(this.getNodeIds(), that.getNodeIds())
                    && Objects.equals(this.getPolicies(), that.getPolicies());
        }
        return false;
    }
}
