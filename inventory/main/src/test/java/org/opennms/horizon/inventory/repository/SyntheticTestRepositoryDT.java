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

package org.opennms.horizon.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.SyntheticTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Developer test to verify schema creation and database operations.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class SyntheticTestRepositoryDT {

    public static final String TENANT_ID = "foo";

    @Autowired
    private SyntheticTransactionRepository syntheticTransactionRepository;
    @Autowired
    private SyntheticTestRepository syntheticTestRepository;

    @BeforeEach
    public void setUp() {
        SyntheticTransaction transaction = new SyntheticTransaction();
        transaction.setTenantId(TENANT_ID);
        transaction.setLabel("test");
        syntheticTransactionRepository.save(transaction);
    }

    @Test
    public void check() {
        List<SyntheticTransaction> transactions = syntheticTransactionRepository.findByTenantId(TENANT_ID);

        assertThat(transactions).isNotNull()
            .hasSize(1);
    }

    @AfterEach
    public void cleanUp(){
        syntheticTestRepository.deleteAll();
        syntheticTransactionRepository.deleteAll();
    }

}
