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
package org.opennms.horizon.minion.taskset.worker;

import com.google.protobuf.Any;

public interface RetryableExecutor {
    /**
     * Initialize the executor for the workflow.
     *
     * @param handleRetryNeeded callback listening for disconnects in order to schedule reconnect attempts.  Only call
     *                         after a successful attempt() call.
     */
    void init(Runnable handleRetryNeeded);

    /**
     * Attempt the executor.  After success, needs to schedule a retry must be triggered by calling the disconnect
     * handler provided at init time.
     *
     * @throws Exception indicate failure of the attempt; another attempt is automatically scheduled.
     */
    void attempt(Any configuration) throws Exception;

    /**
     * Cancel the executor on shutdown of the workflow.
     */
    void cancel();
}
