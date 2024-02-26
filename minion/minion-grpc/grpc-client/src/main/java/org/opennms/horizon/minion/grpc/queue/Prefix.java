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

import com.google.common.base.MoreObjects;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class Prefix {
    private final byte[] bytes;

    public Prefix(final String prefix) {
        this.bytes = prefix.getBytes(StandardCharsets.UTF_8);
    }

    public Prefix(final byte[] prefix) {
        this.bytes = Objects.requireNonNull(prefix);
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
        int length = this.bytes.length + 1;
        for (byte[] array : that) {
            length += array.length;
        }

        byte[] result = new byte[length];

        System.arraycopy(this.bytes, 0, result, 0, this.bytes.length);

        // Add separator after prefix
        result[this.bytes.length] = '$';

        int pos = this.bytes.length + 1;
        for (byte[] array : that) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }

        return result;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("bytes", bytes).toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final Prefix that)) {
            return false;
        }
        return Arrays.equals(this.bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }
}
