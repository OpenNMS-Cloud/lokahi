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
package org.opennms.miniongateway.taskset.service;

import org.opennms.taskset.contract.TaskSet;

public interface TaskSetStorageUpdateFunction {
    /**
     * Process the original task set given and return the updated task set to store.
     *
     * CRITICAL SECTION WARNING: this method is called with a distributed lock held.  Keep implementations short and sweet.
     *
     * @param original copy of the task set from storage.
     * @return (1) the updated task set, (2) the original to indicate that no changes need to be stored, or (3) null
     *         to indicate the task set should be removed.
     */
    TaskSet process(TaskSet original);
}
