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
package org.opennms.horizon.shared.grpc.common;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import java.io.IOException;
import java.util.Properties;

/**
 *  This Interface allows us to have a common Grpc Server for all IPC Services.
 */
public interface GrpcIpcServer {

    /**
     * Starts server, this will not immediately start server but schedules server start after certain delay.
     *
     * @param bindableService The service that needs to be added */
    void startServer(BindableService bindableService) throws IOException;

    /**
     * Starts server, this will not immediately start server but schedules server start after certain delay.
     *
     * @param bindableService The service that needs to be added */
    void startServerWithInterceptors(BindableService bindableService, ServerInterceptor... interceptors)
            throws IOException;

    /**
     * Stops the Server.
     **/
    void stopServer();

    /**
     * Get properties with which the service has started.
     **/
    Properties getProperties();
}
