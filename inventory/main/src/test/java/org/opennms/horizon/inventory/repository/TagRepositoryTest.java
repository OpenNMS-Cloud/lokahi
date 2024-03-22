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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled
public class TagRepositoryTest {

    private static final String DEFAULT_TAG_NAME = "default";
    private static final String DEFAULT_TENANT_ID = "opennms-prime";

    @Autowired
    private TagRepository tagRepository;

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
            .withDatabaseName("inventory")
            .withUsername("inventory")
            .withPassword("password")
            .withExposedPorts(5432);

    static {
        postgres.start();
    }

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
    void throwExceptionOnDuplicateTagForTenant() {

        tagRepository.save(createTag(DEFAULT_TAG_NAME, DEFAULT_TENANT_ID));

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () -> {
            tagRepository.save(createTag(DEFAULT_TAG_NAME, DEFAULT_TENANT_ID));
        });

        // Verify that the exception message contains information about the unique constraint violation
        String expectedMessage = "duplicate key value violates unique constraint \"unique_tag_name-for_tenant\"";
        String actualMessage = exception.getRootCause().getMessage();
        assert actualMessage.contains(expectedMessage) : "Exception message does not contain the expected message.";
    }

    private Tag createTag(String name, String tenantId) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setTenantId(tenantId);
        return tag;
    }
}
