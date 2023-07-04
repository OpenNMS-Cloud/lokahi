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

package org.opennms.horizon.alertservice.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled("Developer test")
public class TagRepositoryTest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
        .withDatabaseName("alerts").withUsername("alerts")
        .withPassword("password").withExposedPorts(5432);
    static {
        postgres.start();
    }

    @Autowired
    private TagRepository tagRepository;


    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> String.format("jdbc:postgresql://localhost:%d/%s", postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setup() {
        assertTrue(postgres.isCreated());
        assertTrue(postgres.isRunning());
    }



    @Test
    public void testTagRepo() {

        Tag tag = new Tag();
        tag.setName("default");
        tag.setTenantId("opennms-prime");
        tag.setNodeIds(List.of(1L,2L,3L));
        tagRepository.save(tag);
        Tag tag1 = new Tag();
        tag1.setName("tag1");
        tag1.setTenantId("opennms-prime");
        tag1.setNodeIds(List.of(3L, 4L, 5L));
        tagRepository.save(tag1);
        var tags = tagRepository.findByTenantIdAndNodeId("opennms-prime", 1L);
        Assertions.assertFalse(tags.isEmpty());
        tags = tagRepository.findByTenantIdAndNodeId("opennms-prime", 8L);
        Assertions.assertTrue(tags.isEmpty());
        tags = tagRepository.findByTenantIdAndNodeId("tenantId", 3L);
        Assertions.assertTrue(tags.isEmpty());
        tags = tagRepository.findByTenantIdAndNodeId("opennms-prime", 3L);
        Assertions.assertEquals(2, tags.size());
        Assertions.assertTrue(tags.stream().map(Tag::getName).toList().contains("tag1"));
    }
}
