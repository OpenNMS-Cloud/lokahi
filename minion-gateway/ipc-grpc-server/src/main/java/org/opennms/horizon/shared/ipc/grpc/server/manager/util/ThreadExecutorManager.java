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
package org.opennms.horizon.shared.ipc.grpc.server.manager.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadExecutorManager {
    private final ThreadFactory timerThreadFactory =
            new ThreadFactoryBuilder().setNameFormat("rpc-timeout-tracker-%d").build();

    // RPC timeout executor thread retrieves elements from delay queue used to timeout rpc requests.
    private final ExecutorService rpcTimeoutExecutor = Executors.newSingleThreadExecutor(timerThreadFactory);

    private final ThreadFactory responseHandlerThreadFactory =
            new ThreadFactoryBuilder().setNameFormat("rpc-response-handler-%d").build();
    private final ExecutorService responseHandlerExecutor = Executors.newCachedThreadPool(responseHandlerThreadFactory);

    // ========================================
    // Getters
    // ----------------------------------------

    public ExecutorService getRpcTimeoutExecutor() {
        return rpcTimeoutExecutor;
    }

    public ExecutorService getResponseHandlerExecutor() {
        return responseHandlerExecutor;
    }
}
