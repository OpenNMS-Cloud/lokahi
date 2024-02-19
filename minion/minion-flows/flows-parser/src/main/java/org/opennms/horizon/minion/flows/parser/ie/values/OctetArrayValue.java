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
package org.opennms.horizon.minion.flows.parser.ie.values;

import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.bytes;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.Optional;
import org.opennms.horizon.minion.flows.parser.InvalidPacketException;
import org.opennms.horizon.minion.flows.parser.MissingTemplateException;
import org.opennms.horizon.minion.flows.parser.ie.InformationElement;
import org.opennms.horizon.minion.flows.parser.ie.InformationElementDatabase;
import org.opennms.horizon.minion.flows.parser.ie.Semantics;
import org.opennms.horizon.minion.flows.parser.ie.Value;
import org.opennms.horizon.minion.flows.parser.session.Session;

public class OctetArrayValue extends Value<byte[]> {
    public final byte[] value;

    public OctetArrayValue(final String name, final Optional<Semantics> semantics, final byte[] value) {
        super(name, semantics);
        this.value = Objects.requireNonNull(value);
    }

    public OctetArrayValue(final String name, final byte[] value) {
        this(name, Optional.empty(), value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", getName())
                .add("data", value)
                .toString();
    }

    public static InformationElement parser(final String name, final Optional<Semantics> semantics) {
        return parserWithLimits(0, 0xFFFF).parser(name, semantics);
    }

    public static InformationElementDatabase.ValueParserFactory parserWithLimits(final int minimum, final int maximum) {
        return (name, semantics) -> new InformationElement() {
            @Override
            public Value<?> parse(Session.Resolver resolver, ByteBuf buffer)
                    throws InvalidPacketException, MissingTemplateException {
                return new OctetArrayValue(name, semantics, bytes(buffer, buffer.readableBytes()));
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getMinimumFieldLength() {
                return minimum;
            }

            @Override
            public int getMaximumFieldLength() {
                return maximum;
            }
        };
    }

    @Override
    public byte[] getValue() {
        return this.value;
    }

    @Override
    public void visit(final Visitor visitor) {
        visitor.accept(this);
    }
}
