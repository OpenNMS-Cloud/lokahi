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
package org.opennms.horizon.minion.grpc.queue;

/**
 * A multi-headed queue.
 *
 * A hydra consists of a global queue and multiple sub-queues. Pushing elements to a hydra is always done using a
 * sub-queue. The added elements will then be added to the sub-queue and the global queue. Taking elements from a
 * sub-queue will also remove that element from the global queue, even if they are not at the head of the global queue.
 * In addition, the global queue can be polled for elements. Elements directly taken from the global queue will still
 * remain in the sub-queue which have been used to enqueue that element.
 */
public interface Hydra<E> {
    E poll();

    SubQueue<E> queue();

    interface SubQueue<E> {
        public E take() throws InterruptedException;

        public E poll();

        public void put(final E element) throws InterruptedException;
    }
}
