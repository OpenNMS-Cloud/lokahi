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

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NaiveHydra<E> implements Hydra<E> {

    private final Queue<E> global = new ConcurrentLinkedQueue<>();

    public NaiveHydra() {}

    @Override
    public E poll() {
        return this.global.poll();
    }

    @Override
    public Hydra.SubQueue<E> queue() {
        return new SubQueue();
    }

    public class SubQueue implements Hydra.SubQueue<E> {
        private final BlockingQueue<E> local = new LinkedBlockingQueue<>();

        private SubQueue() {}

        public E take() throws InterruptedException {
            final var element = this.local.take();
            NaiveHydra.this.global.remove(element);
            return element;
        }

        public E poll() {
            final var element = this.local.poll();
            NaiveHydra.this.global.remove(element);
            return element;
        }

        public void put(final E element) throws InterruptedException {
            NaiveHydra.this.global.offer(element);
            this.local.put(element);
        }
    }
}
