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
package org.opennms.horizon.alertservice.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled("Developer test")
public class TagRepositoryTest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
            .withDatabaseName("alerts")
            .withUsername("alerts")
            .withPassword("password")
            .withExposedPorts(5432);

    static {
        postgres.start();
    }

    @Autowired
    private TagRepository tagRepository;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> String.format(
                        "jdbc:postgresql://localhost:%d/%s",
                        postgres.getFirstMappedPort(), postgres.getDatabaseName()));
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
        tag.setNodeIds(List.of(1L, 2L, 3L));
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
