/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.parser.transport;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.opennms.horizon.minion.flows.parser.ie.values.BooleanValue;
import org.opennms.horizon.minion.flows.parser.ie.values.DateTimeValue;
import org.opennms.horizon.minion.flows.parser.ie.values.IPv4AddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.IPv6AddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.UnsignedValue;

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import org.opennms.horizon.minion.flows.parser.ie.Value;

public class MessageUtils {

    static Long getLongValue(Value<?> value) {
        if (value instanceof UnsignedValue) {
            UnsignedLong unsignedValue = ((UnsignedValue) value).getValue();
            return unsignedValue.longValue();
        }
        return null;
    }


    static Boolean getBooleanValue(Value<?> value) {
        return value instanceof BooleanValue ? ((BooleanValue) value).getValue() : Boolean.FALSE;
    }

    static InetAddress getInetAddress(Value<?> value) {
        if (value instanceof IPv4AddressValue) {
            return (InetAddress) value.getValue();
        }
        return value instanceof IPv6AddressValue ? (InetAddress) value.getValue() : null;
    }

    static Instant getTime(Value<?> value) {
        return value instanceof DateTimeValue ? ((DateTimeValue) value).getValue() : null;
    }

    public static <V> Optional<V> first(final V... values) {
        return Stream.of(values)
                .filter(Objects::nonNull)
                .findFirst();
    }


    static Optional<UInt64Value> getUInt64Value(Value<?> value) {
        Long longValue = getLongValue(value);
        return longValue != null ? Optional.of(UInt64Value.newBuilder().setValue(longValue).build()) : Optional.empty();
    }

    static Optional<UInt64Value> getUInt64Value(Long value) {
        return value != null ? Optional.of(UInt64Value.newBuilder().setValue(value).build()) : Optional.empty();
    }


    static Optional<UInt32Value> getUInt32Value(Value<?> value) {
        Long longValue = getLongValue(value);
        return longValue != null ? Optional.of(UInt32Value.newBuilder().setValue(longValue.intValue()).build()) : Optional.empty();
    }


    static Optional<DoubleValue> getDoubleValue(Value<?> value) {
        Long longValue = getLongValue(value);
        return longValue != null ? Optional.of(DoubleValue.newBuilder().setValue(longValue.doubleValue()).build()) : Optional.empty();
    }

    static UInt32Value setIntValue(int value) {
        return UInt32Value.newBuilder().setValue(value).build();
    }

    static UInt64Value setLongValue(long value) {
        return UInt64Value.newBuilder().setValue(value).build();
    }

    static DoubleValue setDoubleValue(double value) {
        return DoubleValue.newBuilder().setValue(value).build();
    }
}
