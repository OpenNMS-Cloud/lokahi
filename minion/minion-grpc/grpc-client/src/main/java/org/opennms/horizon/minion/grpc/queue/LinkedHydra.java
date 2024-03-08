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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedHydra<E> implements Hydra<E> {

    private final Lock lock = new ReentrantLock();

    private static class Node<E> {
        public Node<E> globalPrev;
        public Node<E> globalNext;

        public Node<E> localNext;

        public E element;
    }

    private transient Node<E> head;
    private transient Node<E> tail;

    @Override
    public E poll() {
        final var lock = LinkedHydra.this.lock;
        lock.lock();
        try {
            final var curr = this.head;
            if (curr == null) {
                return null;
            }

            this.head = this.head.globalNext;
            if (this.head == null) {
                this.tail = null;
            }

            curr.globalPrev = null;

            return curr.element;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public Hydra.SubQueue<E> queue() {
        return new SubQueue();
    }

    public class SubQueue implements Hydra.SubQueue<E> {

        private final Condition available = LinkedHydra.this.lock.newCondition();

        private transient Node<E> head;
        private transient Node<E> tail;

        @Override
        public E take() throws InterruptedException {
            final var lock = LinkedHydra.this.lock;
            lock.lock();
            try {
                while (this.head == null) {
                    available.await();
                }

                final var element = this.dequeue();

                if (this.head != null) {
                    this.available.signal();
                }

                return element;

            } finally {
                lock.unlock();
            }
        }

        @Override
        public E poll() {
            final var lock = LinkedHydra.this.lock;
            lock.lock();
            try {
                if (this.head == null) {
                    return null;
                }

                final var element = this.dequeue();

                if (this.head != null) {
                    this.available.signal();
                }

                return element;

            } finally {
                lock.unlock();
            }
        }

        @Override
        public void put(final E element) throws InterruptedException {
            final var lock = LinkedHydra.this.lock;
            lock.lock();
            try {
                this.enqueue(element);

                this.available.signal();
            } finally {
                lock.unlock();
            }
        }

        private void enqueue(final E element) {
            final Node<E> newNode = new Node<>();
            newNode.globalNext = null;
            newNode.globalPrev = LinkedHydra.this.tail;
            newNode.localNext = null;
            newNode.element = element;

            if (this.tail == null) {
                this.head = newNode;
            } else {
                this.tail.localNext = newNode;
            }

            if (LinkedHydra.this.tail == null) {
                LinkedHydra.this.head = newNode;
            } else {
                LinkedHydra.this.tail.globalNext = newNode;
            }

            this.tail = newNode;
            LinkedHydra.this.tail = newNode;
        }

        private E dequeue() {
            final var curr = this.head;

            this.head = this.head.localNext;
            if (this.head == null) {
                this.tail = null;
            }

            if (curr.globalPrev != null) {
                curr.globalPrev.globalNext = curr.globalNext;
            } else {
                LinkedHydra.this.head = curr.globalNext;
            }

            if (curr.globalNext != null) {
                curr.globalNext.globalPrev = curr.globalPrev;
            } else {
                LinkedHydra.this.tail = curr.globalPrev;
            }

            final var element = curr.element;
            curr.element = null;
            curr.globalPrev = null;
            curr.globalNext = null;
            curr.localNext = null;

            return element;
        }
    }
}
