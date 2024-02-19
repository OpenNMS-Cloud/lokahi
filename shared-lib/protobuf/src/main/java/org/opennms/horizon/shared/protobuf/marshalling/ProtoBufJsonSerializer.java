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
package org.opennms.horizon.shared.protobuf.marshalling;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.lang.reflect.Method;

public class ProtoBufJsonSerializer<T extends Message> extends JsonSerializer<T> {
    Class clazz;

    public ProtoBufJsonSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> handledType() {
        return clazz;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            Method getDescriptorMethod = clazz.getMethod("getDescriptor");
            Descriptors.Descriptor descriptor = (Descriptors.Descriptor) getDescriptorMethod.invoke(null);

            TypeRegistry typeRegistry =
                    TypeRegistry.newBuilder().add(descriptor).build();

            gen.writeRaw(JsonFormat.printer().usingTypeRegistry(typeRegistry).print(value));
        } catch (Exception exc) {
            throw new RuntimeException("failed to serialize protobuf message: type-name=" + clazz.getTypeName(), exc);
        }
    }
}
