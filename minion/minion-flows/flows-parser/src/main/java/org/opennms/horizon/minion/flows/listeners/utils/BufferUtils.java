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
package org.opennms.horizon.minion.flows.listeners.utils;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import io.netty.buffer.ByteBuf;
import java.nio.BufferUnderflowException;
import java.util.function.Function;

public final class BufferUtils {

    private BufferUtils() {}

    public static ByteBuf slice(final ByteBuf buffer, final int size) {
        if (size > buffer.readableBytes()) {
            throw new BufferUnderflowException();
        }

        final ByteBuf result = buffer.slice(buffer.readerIndex(), size);
        buffer.readerIndex(buffer.readerIndex() + size);

        return result;
    }

    public static <R> R peek(final ByteBuf buffer, Function<ByteBuf, R> consumer) {
        final int position = buffer.readerIndex();
        try {
            return consumer.apply(buffer);
        } finally {
            buffer.readerIndex(position);
        }
    }

    public static float sfloat(final ByteBuf buffer) {
        return Float.intBitsToFloat(sint32(buffer));
    }

    public static UnsignedLong uint(final ByteBuf buffer, final int octets) {
        Preconditions.checkArgument(0 <= octets && octets <= 8);

        long result = 0;

        for (int i = 0; i < octets; i++) {
            result = (result << 8L) | (buffer.readUnsignedByte() & 0xFFL);
        }

        return UnsignedLong.fromLongBits(result);
    }

    public static Long sint(final ByteBuf buffer, final int octets) {
        Preconditions.checkArgument(0 <= octets && octets <= 8);

        long result = buffer.readUnsignedByte() & 0xFFL;
        boolean s = (result & 0x80L) != 0;
        if (s) {
            result = 0xFFFFFFFFFFFFFF80L | (result & 0x7FL);
        } else {
            result &= 0x7FL;
        }

        for (int i = 1; i < octets; i++) {
            result = (result << 8L) | (buffer.readUnsignedByte() & 0xFFL);
        }

        return result;
    }

    public static int uint8(final ByteBuf buffer) {
        return buffer.readUnsignedByte() & 0xFF;
    }

    public static int uint16(final ByteBuf buffer) {
        return ((buffer.readUnsignedByte() & 0xFF) << 8) | (buffer.readUnsignedByte() & 0xFF);
    }

    public static int uint24(final ByteBuf buffer) {
        return ((buffer.readUnsignedByte() & 0xFF) << 16)
                | ((buffer.readUnsignedByte() & 0xFF) << 8)
                | (buffer.readUnsignedByte() & 0xFF);
    }

    public static long uint32(final ByteBuf buffer) {
        return ((buffer.readUnsignedByte() & 0xFFL) << 24)
                | ((buffer.readUnsignedByte() & 0xFFL) << 16)
                | ((buffer.readUnsignedByte() & 0xFFL) << 8)
                | (buffer.readUnsignedByte() & 0xFFL);
    }

    public static UnsignedLong uint64(final ByteBuf buffer) {
        return UnsignedLong.fromLongBits(((buffer.readUnsignedByte() & 0xFFL) << 56)
                | ((buffer.readUnsignedByte() & 0xFFL) << 48)
                | ((buffer.readUnsignedByte() & 0xFFL) << 40)
                | ((buffer.readUnsignedByte() & 0xFFL) << 32)
                | ((buffer.readUnsignedByte() & 0xFFL) << 24)
                | ((buffer.readUnsignedByte() & 0xFFL) << 16)
                | ((buffer.readUnsignedByte() & 0xFFL) << 8)
                | (buffer.readUnsignedByte() & 0xFFL));
    }

    public static Integer sint32(final ByteBuf buffer) {
        return ((buffer.readUnsignedByte() & 0xFF) << 24)
                | ((buffer.readUnsignedByte() & 0xFF) << 16)
                | ((buffer.readUnsignedByte() & 0xFF) << 8)
                | (buffer.readUnsignedByte() & 0xFF);
    }

    public static byte[] bytes(final ByteBuf buffer, final int size) {
        final byte[] result = new byte[size];
        buffer.readBytes(result);
        return result;
    }

    @FunctionalInterface
    public interface Parser<T, E extends Exception> {
        T parse(final ByteBuf buffer) throws E;
    }
}
