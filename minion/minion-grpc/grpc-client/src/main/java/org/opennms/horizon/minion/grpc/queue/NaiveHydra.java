/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

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

        @Override
        public boolean isEmpty() {
            return this.local.isEmpty();
        }
    }
}
