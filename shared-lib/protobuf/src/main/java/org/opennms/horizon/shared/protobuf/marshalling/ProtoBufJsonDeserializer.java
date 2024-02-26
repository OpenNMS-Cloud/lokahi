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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.Data;

@Data
public class ProtoBufJsonDeserializer<T extends Message> extends JsonDeserializer {

    private final Class<T> clazz;

    @Override
    public Class<?> handledType() {
        return clazz;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {

        try {
            JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);

            Method newBuilderMethod = clazz.getMethod("newBuilder");
            Builder builder = (Builder) newBuilderMethod.invoke(null);

            Method getDescriptorMethod = clazz.getMethod("getDescriptor");
            Descriptors.Descriptor descriptor = (Descriptors.Descriptor) getDescriptorMethod.invoke(null);

            TypeRegistry typeRegistry =
                    TypeRegistry.newBuilder().add(descriptor).build();
            JsonFormat.parser().usingTypeRegistry(typeRegistry).merge(jsonNode.toString(), builder);

            return (T) builder.build();

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("failed to deserialize protobuf message: type-name=" + clazz.getTypeName(), e);
        }
    }
}
