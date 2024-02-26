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
package org.opennms.horizon.minion.flows.parser.netflow9.proto;

import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.uint16;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opennms.horizon.minion.flows.parser.InvalidPacketException;
import org.opennms.horizon.minion.flows.parser.MissingTemplateException;
import org.opennms.horizon.minion.flows.parser.Protocol;
import org.opennms.horizon.minion.flows.parser.ie.InformationElement;
import org.opennms.horizon.minion.flows.parser.ie.InformationElementDatabase;
import org.opennms.horizon.minion.flows.parser.ie.Value;
import org.opennms.horizon.minion.flows.parser.ie.values.UndeclaredValue;
import org.opennms.horizon.minion.flows.parser.session.Field;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FieldSpecifier implements Field {
    private static final Logger LOG = LoggerFactory.getLogger(FieldSpecifier.class);

    /*
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |        Field Type N           |         Field Length N        |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    */

    public static final int SIZE = 4;

    public final int fieldType; // uint16
    public final int fieldLength; // uint16

    public final InformationElement informationElement;

    public FieldSpecifier(final ByteBuf buffer) throws InvalidPacketException {
        this.fieldType = uint16(buffer);
        this.fieldLength = uint16(buffer);

        this.informationElement = InformationElementDatabase.instance
                .lookup(Protocol.NETFLOW9, this.fieldType)
                .orElseGet(() -> {
                    LOG.warn("Undeclared field type: {}", UndeclaredValue.nameFor(Optional.empty(), this.fieldType));
                    return UndeclaredValue.parser(this.fieldType);
                });

        if (this.fieldLength > this.informationElement.getMaximumFieldLength()
                || this.fieldLength < this.informationElement.getMinimumFieldLength()) {
            throw new InvalidPacketException(
                    buffer,
                    "Template field '%s' has illegal size: %d (min=%d, max=%d)",
                    this.informationElement.getName(),
                    this.fieldLength,
                    this.informationElement.getMinimumFieldLength(),
                    this.informationElement.getMaximumFieldLength());
        }
    }

    @Override
    public Value<?> parse(final Session.Resolver resolver, final ByteBuf buffer)
            throws InvalidPacketException, MissingTemplateException {
        return this.informationElement.parse(resolver, buffer);
    }

    @Override
    public int length() {
        return this.fieldLength;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fieldType", fieldType)
                .add("fieldLength", fieldLength)
                .toString();
    }
}
