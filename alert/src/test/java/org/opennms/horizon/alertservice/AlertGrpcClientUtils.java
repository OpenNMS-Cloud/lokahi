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
package org.opennms.horizon.alertservice;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import java.util.concurrent.TimeUnit;
import org.opennms.horizon.alert.tag.proto.TagServiceGrpc;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionServiceGrpc;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;

public class AlertGrpcClientUtils {
    private static final int DEADLINE_DURATION = 30000;
    private static final String LOCALHOST = "localhost";

    private final DynamicTenantIdInterceptor dynamicTenantIdInterceptor = new DynamicTenantIdInterceptor(
            // Pull private key directly from container
            CucumberRunnerIT.testContainerRunnerClassRule.getJwtKeyPair());
    private AlertServiceGrpc.AlertServiceBlockingStub alertServiceStub;
    private MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub policyStub;
    private TagServiceGrpc.TagServiceBlockingStub tagStub;
    private AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceBlockingStub alertEventDefinitionStub;

    public AlertGrpcClientUtils() {
        initStubs();
    }

    private void initStubs() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(
                LOCALHOST,
                // Pull gRPC server port directly from container
                CucumberRunnerIT.testContainerRunnerClassRule.getGrpcPort());

        ManagedChannel managedChannel = channelBuilder.usePlaintext().build();
        managedChannel.getState(true);
        alertServiceStub = AlertServiceGrpc.newBlockingStub(managedChannel)
                .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
                .withInterceptors(dynamicTenantIdInterceptor);
        policyStub = MonitorPolicyServiceGrpc.newBlockingStub(managedChannel)
                .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
                .withInterceptors(dynamicTenantIdInterceptor);
        tagStub = TagServiceGrpc.newBlockingStub(managedChannel)
                .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
                .withInterceptors(dynamicTenantIdInterceptor);
        alertEventDefinitionStub = AlertEventDefinitionServiceGrpc.newBlockingStub(managedChannel)
                .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
                .withInterceptors(dynamicTenantIdInterceptor);
    }

    public void setTenantId(String tenantId) {
        dynamicTenantIdInterceptor.setTenantId(tenantId);
    }

    public AlertServiceGrpc.AlertServiceBlockingStub getAlertServiceStub() {
        return alertServiceStub;
    }

    public MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub getPolicyStub() {
        return policyStub;
    }

    public TagServiceGrpc.TagServiceBlockingStub getTagStub() {
        return tagStub;
    }

    public AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceBlockingStub getAlertEventDefinitionStub() {
        return alertEventDefinitionStub;
    }
}
