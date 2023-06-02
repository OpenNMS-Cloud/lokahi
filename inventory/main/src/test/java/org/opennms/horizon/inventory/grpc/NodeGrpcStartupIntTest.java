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

package org.opennms.horizon.inventory.grpc;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

@SpringBootTest(properties = {"spring.liquibase.change-log=db/changelog/changelog-test.xml"})
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@AutoConfigureObservability     // Make sure to include Metrics (for some reason they are disabled by default in the integration grey-box test)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled
class NodeGrpcStartupIntTest extends GrpcTestBase {
    @Autowired
    private ApplicationContext context;
    private TestTaskSetGrpcService testGrpcService;
    private static final int EXPECTED_TASK_DEF_COUNT = 2;

    @BeforeEach
    public void prepare() {
        testGrpcService = context.getBean(TestTaskSetGrpcService.class);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        //this test have to clean after tests.
        testGrpcService.reset();
        afterTest();
    }

    @Test
    void testStartup() {
        // TrapConfigService & FlowsConfigService listens for ApplicationReadyEvent and sends the trap config for each location.
        await().atMost(15, TimeUnit.SECONDS).until(() -> testGrpcService.getRequests().size(), Matchers.is(2));

        org.assertj.core.api.Assertions.assertThat(testGrpcService.getRequests())
            .hasSize(2)
            .extracting((ele) -> ((UpdateTasksRequest)ele).getUpdateList())
            .hasSize(EXPECTED_TASK_DEF_COUNT);
    }
}
