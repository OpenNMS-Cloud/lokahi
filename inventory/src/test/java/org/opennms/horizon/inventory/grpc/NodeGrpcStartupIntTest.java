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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = { "spring.liquibase.change-log=db/changelog/changelog-test.xml" })
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class NodeGrpcStartupIntTest extends GrpcTestBase {
    private static final int EXPECTED_TASK_DEF_COUNT = 1;

    @Autowired
    private IpInterfaceRepository ipInterfaceRepository;
    @Autowired
    private NodeRepository nodeRepository;

    private static TestTaskSetGrpcService testGrpcService;

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();

        ipInterfaceRepository.deleteAll();
        nodeRepository.deleteAll();
    }

    @BeforeAll
    public static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @AfterEach
    public void cleanUp() {
        testGrpcService.reset();
        channel.shutdown();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @Test
    void testStartup() throws Exception {
        // TrapConfigService & FlowsConfigService listens for ApplicationReadyEvent and sends the trap config for each location.
        await().atMost(10, TimeUnit.SECONDS).untilAtomic(testGrpcService.getTimesCalled(), Matchers.is(2));

        org.assertj.core.api.Assertions.assertThat(testGrpcService.getRequests())
            .hasSize(2)
            .extracting(PublishTaskSetRequest::getTaskSet)
            .isNotNull()
            .extracting(TaskSet::getTaskDefinitionCount)
            .contains(EXPECTED_TASK_DEF_COUNT);
    }
}
