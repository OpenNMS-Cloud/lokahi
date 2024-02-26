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
package org.opennms.horizon.shared.grpc.interceptor;

import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.micrometer.core.instrument.MeterRegistry;

public class MeteringInterceptorFactory implements InterceptorFactory {

    private final MeterRegistry meterRegistry;

    public MeteringInterceptorFactory(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public BindableService create(BindableService service) {
        ServerServiceDefinition definition = ServerInterceptors.intercept(
                ServerInterceptors.useInputStreamMessages(service.bindService()),
                new MeteringServerInterceptor(this.meterRegistry));
        return new BindableService() {
            @Override
            public ServerServiceDefinition bindService() {
                return definition;
            }
        };
    }
}
