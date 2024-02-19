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
package org.opennms.horizon.shared.protobuf.util;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ProtobufUtil {
    private ProtobufUtil() {
        // block static class constructor
    }

    public static <T extends GeneratedMessageV3> T fromJson(String json, Class<T> clazz)
            throws InvalidProtocolBufferException {
        T.Builder<?> builder = null;
        try {
            Method m = clazz.getMethod("newBuilder");
            builder = (T.Builder) m.invoke(null);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new InvalidProtocolBufferException("newBuilder is missing.");
        }
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        return (T) builder.build();
    }

    public static String toJson(MessageOrBuilder messageOrBuilder) throws InvalidProtocolBufferException {
        return JsonFormat.printer().print(messageOrBuilder);
    }
}
