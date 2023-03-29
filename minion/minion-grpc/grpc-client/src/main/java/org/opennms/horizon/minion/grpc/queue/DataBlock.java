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

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;

public interface DataBlock {
    static Container memory(final byte[] key, final ArrayDeque<Map.Entry<String, byte[]>> data) {
        return new Container(new MemoryDataBlock(key, data));
    }

//    boolean offer(final T element);
//
//    T poll();
//
//    int size();

//    default boolean isEmpty() {
//        return this.size() == 0;
//    }

    MemoryDataBlock asMemory();

    OffHeapDataBlock asOffHeap();

    public static class Container {
        private DataBlock block;

        public Container(final DataBlock block) {
            this.block = Objects.requireNonNull(block);
        }

        void toMemory() {
            this.block = this.block.asMemory();
        }

        void toOffHeap() {
            this.block = this.block.asOffHeap();
        }
    }

}
