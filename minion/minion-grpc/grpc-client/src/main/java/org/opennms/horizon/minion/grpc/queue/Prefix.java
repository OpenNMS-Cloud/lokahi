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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.google.common.primitives.Bytes;

public class Prefix {
    private final byte[] bytes;

    public Prefix(final String prefix) {
        this.bytes = (prefix + "$").getBytes(StandardCharsets.UTF_8);
    }

    /** Checks if that starts with this prefix.
     *
     * @param that the bytes to check for this prefix.
     * @return {@code true}, if that starts with this, {@code false} otherwise.
     */
    public boolean of(final byte[] that) {
        if (that == null || that.length < this.bytes.length) {
            return false;
        }

        for (int i = 0; i < this.bytes.length; i++) {
            if (that[i] != this.bytes[i]) {
                return false;
            }
        }

        return true;
    }

    /** Prefixes that with this.
     *
     * @param that the bytes to prefix with this.
     * @return that prefixed with this.
     */
    public byte[] with(final byte[]... that) {
        int length = this.bytes.length;
        for (byte[] array : that) {
            length += array.length;
        }

        byte[] result = new byte[length];

        System.arraycopy(this.bytes, 0, result, 0, this.bytes.length);
        int pos = this.bytes.length;

        for (byte[] array : that) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }

        return result;
    }

    public byte[] getBytes() {
        return this.bytes;
    }
}
