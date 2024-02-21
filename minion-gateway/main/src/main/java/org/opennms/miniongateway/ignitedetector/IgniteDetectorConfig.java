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
package org.opennms.miniongateway.ignitedetector;

import org.apache.ignite.Ignite;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestDispatcher;
import org.opennms.miniongateway.detector.api.LocalDetectorAdapter;
import org.opennms.miniongateway.detector.server.IgniteRpcRequestDispatcher;
import org.opennms.miniongateway.detector.server.LocalDetectorAdapterStubImpl;
import org.opennms.miniongateway.ignite.LocalIgniteRpcRequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteDetectorConfig {
    private final Logger logger = LoggerFactory.getLogger(IgniteDetectorConfig.class);

    @Autowired
    private Ignite ignite;

    @Bean("localDetectorAdapter")
    public LocalDetectorAdapter localDetectorAdapter() {
        return new LocalDetectorAdapterStubImpl();
    }

    @Bean("igniteRpcRequestDispatcher")
    public IgniteRpcRequestDispatcher requestDispatcher(RpcRequestDispatcher requestDispatcher) {
        return new LocalIgniteRpcRequestDispatcher(requestDispatcher);
    }
}
