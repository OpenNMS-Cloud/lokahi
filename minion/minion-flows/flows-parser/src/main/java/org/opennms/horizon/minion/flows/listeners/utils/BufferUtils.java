/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.listeners.utils;

import java.nio.BufferUnderflowException;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

import io.netty.buffer.ByteBuf;

public final class BufferUtils {

    private BufferUtils() {
    }

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
        return ((buffer.readUnsignedByte() & 0xFF) << 8)
             | (buffer.readUnsignedByte() & 0xFF);
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
        return UnsignedLong.fromLongBits(
                ((buffer.readUnsignedByte() & 0xFFL) << 56)
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
