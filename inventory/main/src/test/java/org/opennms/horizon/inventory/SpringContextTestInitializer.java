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

package org.opennms.horizon.inventory;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.opennms.horizon.inventory.component.TagPublisher;
import org.opennms.horizon.inventory.config.MinionGatewayGrpcClientConfig;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

public class SpringContextTestInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
    public static String GRPC_SERVICE_BEAN = "test-grpc-service";


    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
        .withDatabaseName("inventory").withUsername("inventory")
        .withPassword("password").withExposedPorts(5432);

    static {
        postgres.start();
    }

    @Override
    public void initialize(@NotNull GenericApplicationContext context) {
        initDatasourceParams(context);
        try {
            initMockGrpcTaskSetService(context);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initDatasourceParams(GenericApplicationContext context) {
        TestPropertyValues.of(
            "spring.datasource.url=" + postgres.getJdbcUrl(),
            "spring.datasource.username=" + postgres.getUsername(),
            "spring.datasource.password=" + postgres.getPassword()
        ).applyTo(context.getEnvironment());
    }

    // Creating this test bean in this class due to errors as we are sharing the context between test classes.
    // The Mock Grpc server was getting a different stub due to other tests interfering.
    // This overrides the appropriate bean from the beginning of when the spring context gets initialized.
    private void initMockGrpcTaskSetService(GenericApplicationContext context) throws IOException, InterruptedException {
        String serverName = InProcessServerBuilder.generateName();
        TestTaskSetGrpcService grpcService = new TestTaskSetGrpcService();
        Server server = InProcessServerBuilder.forName(serverName).directExecutor()
            .addService(grpcService).build();
        server.start();
        Thread.sleep(5000);
        registerBean(context, MinionGatewayGrpcClientConfig.MINION_GATEWAY_GRPC_CHANNEL, ManagedChannel.class,
            () -> InProcessChannelBuilder.forName(serverName).directExecutor().build()
        );
        registerBean(context, GRPC_SERVICE_BEAN, TestTaskSetGrpcService.class, () -> grpcService);
        TagPublisher tagPublisher = mock(TagPublisher.class);
        registerBean(context, "tagPublisher", TagPublisher.class, () -> tagPublisher);
    }

    private <T> void registerBean(GenericApplicationContext context, String name, Class<T> clazz, Supplier<T> supplier) {
        BeanDefinitionCustomizer customizer = beanDefinition -> beanDefinition.setPrimary(true);
        context.registerBean(name, clazz, supplier, customizer);
    }
}
