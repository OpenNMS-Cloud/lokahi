/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

import org.opennms.horizon.inventory.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTenantIdAndId(String tenantId, Long id);

    Optional<Tag> findByTenantIdAndName(String tenantId, String name);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.nodes node " +
        "WHERE tag.tenantId = :tenantId " +
        "AND node.id = :nodeId " +
        "AND tag.name = :name")
    Optional<Tag> findByTenantIdNodeIdAndName(@Param("tenantId") String tenantId,
                                              @Param("nodeId") Long nodeId,
                                              @Param("name") String name);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.nodes node " +
        "WHERE tag.tenantId = :tenantId " +
        "AND node.id = :nodeId ")
    List<Tag> findByTenantIdAndNodeId(@Param("tenantId") String tenantId,
                                      @Param("nodeId") long nodeId);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "WHERE tag.tenantId = :tenantId " +
        "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndNameLike(@Param("tenantId") String tenantId,
                                        @Param("searchTerm") String searchTerm);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.nodes node " +
        "WHERE tag.tenantId = :tenantId " +
        "AND node.id = :nodeId " +
        "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndNodeIdAndNameLike(@Param("tenantId") String tenantId,
                                                 @Param("nodeId") long nodeId,
                                                 @Param("searchTerm") String searchTerm);

    List<Tag> findByTenantId(String tenantId);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.azureCredentials azureCredentials " +
        "WHERE tag.tenantId = :tenantId " +
        "AND azureCredentials.id = :azureCredentialId " +
        "AND tag.name = :name")
    Optional<Tag> findByTenantIdAzureCredentialIdAndName(@Param("tenantId") String tenantId,
                                                         @Param("azureCredentialId") Long azureCredentialId,
                                                         @Param("name") String name);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.azureCredentials azureCredential " +
        "WHERE tag.tenantId = :tenantId " +
        "AND azureCredential.id = :azureCredentialId ")
    List<Tag> findByTenantIdAndAzureCredentialId(@Param("tenantId") String tenantId,
                                                 @Param("azureCredentialId") long azureCredentialId);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.azureCredentials azureCredential " +
        "WHERE tag.tenantId = :tenantId " +
        "AND azureCredential.id = :azureCredentialId " +
        "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndAzureCredentialIdAndNameLike(@Param("tenantId") String tenantId,
                                                            @Param("azureCredentialId") long azureCredentialId,
                                                            @Param("searchTerm") String searchTerm);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.passiveDiscoveries discovery " +
        "WHERE tag.tenantId = :tenantId " +
        "AND discovery.id = :passiveDiscoveryId " +
        "AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tag> findByTenantIdAndPassiveDiscoveryIdAndNameLike(@Param("tenantId") String tenantId,
                                                             @Param("passiveDiscoveryId") long passiveDiscoveryId,
                                                             @Param("searchTerm") String searchTerm);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.passiveDiscoveries discovery " +
        "WHERE tag.tenantId = :tenantId " +
        "AND discovery.id = :passiveDiscoveryId ")
    List<Tag> findByTenantIdAndPassiveDiscoveryId(@Param("tenantId") String tenantId,
                                                  @Param("passiveDiscoveryId") long passiveDiscoveryId);

    @Query("SELECT tag " +
        "FROM Tag tag " +
        "JOIN tag.passiveDiscoveries discovery " +
        "WHERE tag.tenantId = :tenantId " +
        "AND discovery.id = :passiveDiscoveryId " +
        "AND tag.name = :name")
    Optional<Tag> findByTenantIdPassiveDiscoveryIdAndName(@Param("tenantId") String tenantId,
                                                          @Param("passiveDiscoveryId") long passiveDiscoveryId,
                                                          @Param("name") String name);
}
