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
package org.opennms.horizon.testtool.miniongateway.wiremock.ipc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MockCloudServiceRunner {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MockCloudServiceRunner.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    private MockCloudServiceConfiguration configuration;

    @Autowired
    private List<BindableService> bindableServiceList;

    private NettyServerBuilder nettyServerBuilder;

    @PostConstruct
    public void start() {
        log.info(
                "STARTING mock cloud service at port {} with max-message-size={}",
                configuration.getPort(),
                configuration.getMaxMessageSize());

        nettyServerBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(configuration.getPort()))
                .maxInboundMessageSize(configuration.getMaxMessageSize())
                .addService(ProtoReflectionService.newInstance());

        if (bindableServiceList != null) {
            log.info("REGISTERING {} GRPC services", bindableServiceList.size());
            bindableServiceList.forEach(nettyServerBuilder::addService);
        } else {
            log.info("HAVE 0 GRPC services");
        }

        Server server = nettyServerBuilder.build();
        try {
            server.start();
        } catch (IOException ioException) {
            throw new RuntimeException("failed to start GRPC server", ioException);
        }
    }
}
