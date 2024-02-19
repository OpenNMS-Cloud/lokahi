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

import java.util.List;
import java.util.Optional;
import org.opennms.horizon.inventory.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTenantIdAndId(String tenantId, Long id);

    Optional<Tag> findByTenantIdAndName(String tenantId, String name);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.nodes node "
            + "WHERE tag.tenantId = :tenantId "
            + "AND node.id = :nodeId "
            + "AND tag.name = :name")
    Optional<Tag> findByTenantIdNodeIdAndName(
            @Param("tenantId") String tenantId, @Param("nodeId") Long nodeId, @Param("name") String name);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.nodes node "
            + "WHERE tag.tenantId = :tenantId "
            + "AND node.id = :nodeId ")
    List<Tag> findByTenantIdAndNodeId(@Param("tenantId") String tenantId, @Param("nodeId") long nodeId);

    @Query("SELECT tag " + "FROM Tag tag "
            + "WHERE tag.tenantId = :tenantId "
            + "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndNameLike(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.nodes node "
            + "WHERE tag.tenantId = :tenantId "
            + "AND node.id = :nodeId "
            + "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndNodeIdAndNameLike(
            @Param("tenantId") String tenantId, @Param("nodeId") long nodeId, @Param("searchTerm") String searchTerm);

    List<Tag> findByTenantId(String tenantId);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.activeDiscoveries discovery "
            + "WHERE tag.tenantId = :tenantId "
            + "AND discovery.id = :discoveryId "
            + "AND tag.name = :name")
    Optional<Tag> findByTenantIdActiveDiscoveryIdAndName(
            @Param("tenantId") String tenantId, @Param("discoveryId") long discoveryId, @Param("name") String name);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.activeDiscoveries discovery "
            + "WHERE tag.tenantId = :tenantId "
            + "AND discovery.id = :discoveryId ")
    List<Tag> findByTenantIdAndActiveDiscoveryId(
            @Param("tenantId") String tenantId, @Param("discoveryId") long discoveryId);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.activeDiscoveries discovery "
            + "WHERE tag.tenantId = :tenantId "
            + "AND discovery.id = :discoveryId "
            + "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndActiveDiscoveryIdAndNameLike(
            @Param("tenantId") String tenantId,
            @Param("discoveryId") long discoveryId,
            @Param("searchTerm") String searchTerm);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.passiveDiscoveries discovery "
            + "WHERE tag.tenantId = :tenantId "
            + "AND discovery.id = :passiveDiscoveryId "
            + "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndPassiveDiscoveryIdAndNameLike(
            @Param("tenantId") String tenantId,
            @Param("passiveDiscoveryId") long passiveDiscoveryId,
            @Param("searchTerm") String searchTerm);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.passiveDiscoveries discovery "
            + "WHERE tag.tenantId = :tenantId "
            + "AND discovery.id = :passiveDiscoveryId ")
    List<Tag> findByTenantIdAndPassiveDiscoveryId(
            @Param("tenantId") String tenantId, @Param("passiveDiscoveryId") long passiveDiscoveryId);

    @Query("SELECT tag " + "FROM Tag tag "
            + "JOIN tag.passiveDiscoveries discovery "
            + "WHERE tag.tenantId = :tenantId "
            + "AND discovery.id = :passiveDiscoveryId "
            + "AND tag.name = :name")
    Optional<Tag> findByTenantIdPassiveDiscoveryIdAndName(
            @Param("tenantId") String tenantId,
            @Param("passiveDiscoveryId") long passiveDiscoveryId,
            @Param("name") String name);
}
