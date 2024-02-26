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
package org.opennms.horizon.minion.scheduler.impl;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledThreadPoolExecutorFactory {
    private AtomicLong threadCounter = new AtomicLong(0);

    public ScheduledThreadPoolExecutor create(int numThread, String threadNamePrefix) {
        ThreadFactory threadFactory = runnable -> {
            Thread result = new Thread(runnable);
            result.setDaemon(true);
            result.setName(formatThreadName(threadNamePrefix));

            return result;
        };

        return new ScheduledThreadPoolExecutor(numThread, threadFactory);
    }

    private String formatThreadName(String prefix) {
        return prefix + threadCounter.getAndIncrement();
    }
}
