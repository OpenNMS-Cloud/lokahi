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
package org.opennms.horizon.minion.flows.parser;

import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.bytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.horizon.minion.flows.parser.ie.Value;
import org.opennms.horizon.minion.flows.parser.ie.values.BooleanValue;
import org.opennms.horizon.minion.flows.parser.ie.values.DateTimeValue;
import org.opennms.horizon.minion.flows.parser.ie.values.FloatValue;
import org.opennms.horizon.minion.flows.parser.ie.values.IPv4AddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.IPv6AddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.ListValue;
import org.opennms.horizon.minion.flows.parser.ie.values.MacAddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.OctetArrayValue;
import org.opennms.horizon.minion.flows.parser.ie.values.SignedValue;
import org.opennms.horizon.minion.flows.parser.ie.values.StringValue;
import org.opennms.horizon.minion.flows.parser.ie.values.UnsignedValue;
import org.opennms.horizon.minion.flows.parser.session.Field;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.Template;

public class ValueTest {

    @Test
    public void testBasicList() throws MissingTemplateException, InvalidPacketException {
        // Given
        ByteBuf byteBuf = Unpooled.buffer()
                .writeByte(0xFF) // Semantic: Undefined
                .writeShort(11) // FieldID: destinationTransportPort (unsigned16)
                .writeShort(2) // Field Length: unsigned16
                .writeBytes(new byte[] {23, 42})
                .writeBytes(new byte[] {13, 37})
                .writeBytes(new byte[] {89, 80});

        // When
        final List<List<Value<?>>> listValue =
                (List<List<Value<?>>>) ListValue.parserWithBasicList("name1", Optional.empty())
                        .parse(null, byteBuf)
                        .getValue();

        // Then
        Assert.assertNotNull(listValue);
        Assert.assertEquals(3, listValue.size());
        Assert.assertEquals(
                23 * 0x100 + 42,
                Long.parseUnsignedLong(
                        ((UnsignedValue) listValue.get(0).get(0)).getValue().toString()));
        Assert.assertEquals(
                13 * 0x100 + 37,
                Long.parseUnsignedLong(
                        ((UnsignedValue) listValue.get(1).get(0)).getValue().toString()));
        Assert.assertEquals(
                89 * 0x100 + 80,
                Long.parseUnsignedLong(
                        ((UnsignedValue) listValue.get(2).get(0)).getValue().toString()));
    }

    @Test
    public void testSubTemplateList() throws MissingTemplateException, InvalidPacketException {
        // Given
        String field1Value = "firstFieldValue";
        String field2Value = "secondFieldValue";

        byte[] field1 = field1Value.getBytes();
        byte[] field2 = field2Value.getBytes();
        List<String> allFieldValues = Arrays.asList(field1Value, field2Value);
        List<String> allFieldNames = Arrays.asList("firstField", "secondField");
        final List<Field> fields = List.of(field("firstField", field1.length), field("secondField", field2.length));
        Session.Resolver resolver = Mockito.mock(Session.Resolver.class);
        Mockito.when(resolver.lookupTemplate(100))
                .thenReturn(Template.builder(100, Template.Type.TEMPLATE)
                        .withFields(fields)
                        .build());

        ByteBuf byteBuf = Unpooled.buffer()
                .writeByte(0xFF) // Semantic: Undefined
                .writeShort(100) // Template ID
                .writeBytes(field1) // Field 1 list 1
                .writeBytes(field2) // Field 2 list 1
                .writeBytes(field1) // Field 1 list 2
                .writeBytes(field2); // Field 2 list 2

        // When
        final List<List<Value<?>>> listValue =
                (List<List<Value<?>>>) ListValue.parserWithSubTemplateList("name1", Optional.empty())
                        .parse(resolver, byteBuf)
                        .getValue();
        Assert.assertEquals(2, listValue.get(0).size());
        Assert.assertEquals(2, listValue.get(1).size());

        // Then
        checkListValues(allFieldValues, allFieldNames, listValue);
    }

    private static Field field(String name, final int length) {
        return new Field() {
            @Override
            public int length() {
                return length;
            }

            @Override
            public Value<?> parse(Session.Resolver resolver, ByteBuf buffer) {
                return new OctetArrayValue(name, bytes(buffer, length));
            }
        };
    }

    @Test
    public void testSubTemplateMultiList() throws MissingTemplateException, InvalidPacketException {
        // Given
        String field1Value = "firstValue";
        String field2Value = "secondValue";
        String field3Value = "thirdValue";
        String field4Value = "fourthValue";
        String field5Value = "fifthValue";

        //  This is the total length of the Data Records encoding for the
        //  Template ID previously specified, including the two bytes for the
        //  Template ID and the two bytes for the Data Records Length field
        //  itself.
        int dataRecordsLength = 4;
        byte[] field1 = field1Value.getBytes();
        byte[] field2 = field2Value.getBytes();
        byte[] field3 = field3Value.getBytes();
        byte[] field4 = field4Value.getBytes();
        byte[] field5 = field5Value.getBytes();
        final List<Field> fields = List.of(field("firstField", field1.length), field("secondField", field2.length));
        final List<Field> fields2 = List.of(
                field("thirdField", field3.length),
                field("fourthField", field4.length),
                field("fifthField", field5.length));

        List<String> allFieldValues = Arrays.asList(field4Value, field2Value, field3Value, field4Value, field5Value);
        List<String> allFieldNames =
                Arrays.asList("firstField", "secondField", "thirdField", "fourthField", "fifthField");
        Session.Resolver resolver = Mockito.mock(Session.Resolver.class);
        Mockito.when(resolver.lookupTemplate(357))
                .thenReturn(Template.builder(357, Template.Type.TEMPLATE)
                        .withFields(fields)
                        .build());

        Mockito.when(resolver.lookupTemplate(358))
                .thenReturn(Template.builder(358, Template.Type.TEMPLATE)
                        .withFields(fields2)
                        .build());

        ByteBuf byteBuf = Unpooled.buffer()
                .writeByte(0xFF) // Semantic: Undefined
                .writeShort(357) // Header ID
                .writeShort(dataRecordsLength + field1.length + field2.length) // Header length
                .writeBytes(field1) // Field 1 template 1
                .writeBytes(field2) // Field 2 template 1
                .writeShort(358)
                .writeShort(dataRecordsLength + field3.length + field4.length + field5.length) // Header length
                .writeBytes(field3) // Field 3 template 2
                .writeBytes(field4) // Field 4 template 2
                .writeBytes(field5); // Field 5 template 2

        // When
        final List<List<Value<?>>> listValue =
                (List<List<Value<?>>>) ListValue.parserWithSubTemplateMultiList("name1", Optional.empty())
                        .parse(resolver, byteBuf)
                        .getValue();
        Assert.assertEquals(2, listValue.get(0).size());
        Assert.assertEquals(3, listValue.get(1).size());

        // Then
        checkListValues(allFieldValues, allFieldNames, listValue);
    }

    private void checkListValues(
            List<String> allFieldValues, List<String> allFieldNames, List<List<Value<?>>> listValue) {
        List<String> foundFieldNames = new ArrayList<>();
        List<String> foundFieldValues = new ArrayList<>();
        Assert.assertNotNull(listValue);

        listValue.forEach(val -> val.forEach(el -> {
            foundFieldNames.add(el.getName());
            foundFieldValues.add(new String((byte[]) el.getValue(), StandardCharsets.UTF_8));
        }));
        Assert.assertTrue(foundFieldNames.containsAll(allFieldNames));
        Assert.assertTrue(foundFieldValues.containsAll(allFieldValues));
    }

    @Test
    public void testBooleanValue() throws Exception {
        final BooleanValue bTrue = (BooleanValue) BooleanValue.parser("booleanName", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer(new byte[] {1}));
        Assert.assertTrue(bTrue.getValue());
        Assert.assertEquals("booleanName", bTrue.getName());

        final BooleanValue bFalse = (BooleanValue) BooleanValue.parser("booleanName", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer(new byte[] {2}));
        Assert.assertFalse(bFalse.getValue());
        Assert.assertEquals("booleanName", bFalse.getName());
    }

    @Test(expected = InvalidPacketException.class)
    public void testBooleanValueInvalid() throws Exception {
        BooleanValue.parser("booleanName", Optional.empty()).parse(null, Unpooled.wrappedBuffer(new byte[] {3}));
    }

    @Test
    public void testDateTimeMicrosecondsValue() throws Exception {
        final DateTimeValue dateTimeMicrosecondsValue1 =
                (DateTimeValue) DateTimeValue.parserWithMicroseconds("dateTimeMicrosecondsName1", Optional.empty())
                        .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
        Assert.assertEquals(
                Instant.from(ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)),
                dateTimeMicrosecondsValue1.getValue());
        Assert.assertEquals("dateTimeMicrosecondsName1", dateTimeMicrosecondsValue1.getName());

        final DateTimeValue dateTimeMicrosecondsName2 =
                (DateTimeValue) DateTimeValue.parserWithMicroseconds("dateTimeMicrosecondsName2", Optional.empty())
                        .parse(
                                null,
                                Unpooled.buffer(8)
                                        .writeInt((int) (1509533996L + DateTimeValue.SECONDS_TO_EPOCH))
                                        .writeInt(0));
        Assert.assertEquals(
                Instant.from(ZonedDateTime.of(2017, 11, 1, 10, 59, 56, 0, ZoneOffset.UTC)),
                dateTimeMicrosecondsName2.getValue());
        Assert.assertEquals("dateTimeMicrosecondsName2", dateTimeMicrosecondsName2.getName());

        final DateTimeValue dateTimeMicrosecondsName3 =
                (DateTimeValue) DateTimeValue.parserWithMicroseconds("dateTimeMicrosecondsName3", Optional.empty())
                        .parse(
                                null,
                                Unpooled.buffer(8)
                                        .writeInt((int) (1509533996L + DateTimeValue.SECONDS_TO_EPOCH))
                                        .writeInt(16775168));
        Assert.assertEquals(
                Instant.from(ZonedDateTime.of(2017, 11, 1, 10, 59, 56, 3905773, ZoneOffset.UTC)),
                dateTimeMicrosecondsName3.getValue());
        Assert.assertEquals("dateTimeMicrosecondsName3", dateTimeMicrosecondsName3.getName());

        final DateTimeValue dateTimeMicrosecondsName4 =
                (DateTimeValue) DateTimeValue.parserWithMicroseconds("dateTimeMicrosecondsName4", Optional.empty())
                        .parse(
                                null,
                                Unpooled.buffer(8)
                                        .writeInt((int) (1509533996L + DateTimeValue.SECONDS_TO_EPOCH))
                                        .writeInt(16775169));
        Assert.assertEquals(
                Instant.from(ZonedDateTime.of(2017, 11, 1, 10, 59, 56, 3905773, ZoneOffset.UTC)),
                dateTimeMicrosecondsName4.getValue());
        Assert.assertEquals("dateTimeMicrosecondsName4", dateTimeMicrosecondsName4.getName());
    }

    @Test
    public void testDateTimeMillisecondsValue() throws Exception {
        final DateTimeValue dateTimeMillisecondsValue1 =
                (DateTimeValue) DateTimeValue.parserWithMilliseconds("dateTimeMillisecondsName1", Optional.empty())
                        .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
        Assert.assertEquals(Instant.ofEpochMilli(0), dateTimeMillisecondsValue1.getValue());
        Assert.assertEquals("dateTimeMillisecondsName1", dateTimeMillisecondsValue1.getName());

        final DateTimeValue dateTimeMillisecondsValue2 =
                (DateTimeValue) DateTimeValue.parserWithMilliseconds("dateTimeMillisecondsName2", Optional.empty())
                        .parse(null, Unpooled.buffer(8).writeLong(1509532300714L));
        Assert.assertEquals(Instant.ofEpochMilli(1509532300714L), dateTimeMillisecondsValue2.getValue());
        Assert.assertEquals("dateTimeMillisecondsName2", dateTimeMillisecondsValue2.getName());
    }

    @Test
    public void testDateTimeNanosecondsValue() throws Exception {
        final DateTimeValue dateTimeNanosecondsValue1 =
                (DateTimeValue) DateTimeValue.parserWithNanoseconds("dateTimeNanosecondsName1", Optional.empty())
                        .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
        Assert.assertEquals(
                Instant.from(ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)),
                dateTimeNanosecondsValue1.getValue());
        Assert.assertEquals("dateTimeNanosecondsName1", dateTimeNanosecondsValue1.getName());

        final DateTimeValue dateTimeNanosecondsValue2 =
                (DateTimeValue) DateTimeValue.parserWithNanoseconds("dateTimeNanosecondsName2", Optional.empty())
                        .parse(
                                null,
                                Unpooled.buffer(8)
                                        .writeInt((int) (1509533996L + DateTimeValue.SECONDS_TO_EPOCH))
                                        .writeInt(0));
        Assert.assertEquals(
                Instant.from(ZonedDateTime.of(2017, 11, 1, 10, 59, 56, 0, ZoneOffset.UTC)),
                dateTimeNanosecondsValue2.getValue());
        Assert.assertEquals("dateTimeNanosecondsName2", dateTimeNanosecondsValue2.getName());

        final DateTimeValue dateTimeNanosecondsValue3 =
                (DateTimeValue) DateTimeValue.parserWithNanoseconds("dateTimeNanosecondsName3", Optional.empty())
                        .parse(
                                null,
                                Unpooled.buffer(8)
                                        .writeInt((int) (1509533996L + DateTimeValue.SECONDS_TO_EPOCH))
                                        .writeInt(34509786));
        Assert.assertEquals(
                Instant.from(ZonedDateTime.of(2017, 11, 1, 10, 59, 56, 8034935, ZoneOffset.UTC)),
                dateTimeNanosecondsValue3.getValue());
        Assert.assertEquals("dateTimeNanosecondsName3", dateTimeNanosecondsValue3.getName());
    }

    @Test
    public void testFloat32Value() throws Exception {
        final FloatValue float32Value1 = (FloatValue) FloatValue.parserWith32Bit("float32Name1", Optional.empty())
                .parse(null, Unpooled.buffer(4).writeFloat(0));
        Assert.assertEquals(0f, float32Value1.getValue(), 0);
        Assert.assertEquals("float32Name1", float32Value1.getName());

        final FloatValue float32Value2 = (FloatValue) FloatValue.parserWith32Bit("float32Name2", Optional.empty())
                .parse(null, Unpooled.buffer(4).writeFloat(123.456f));
        Assert.assertEquals(123.456f, float32Value2.getValue(), 0);
        Assert.assertEquals("float32Name2", float32Value2.getName());

        Assert.assertEquals(
                1.4E-45f,
                ((FloatValue) FloatValue.parserWith32Bit("float32Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1.4E-45f,
                ((FloatValue) FloatValue.parserWith32Bit("float32Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1.4E-45f,
                ((FloatValue) FloatValue.parserWith32Bit("float32Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1.4E-45f,
                ((FloatValue) FloatValue.parserWith32Bit("float32Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue(),
                0);
    }

    @Test
    public void testFloat64Value() throws Exception {
        final FloatValue float64Value1 = (FloatValue) FloatValue.parserWith64Bit("float64Name1", Optional.empty())
                .parse(null, Unpooled.buffer(8).writeDouble(0));
        Assert.assertEquals(0f, float64Value1.getValue(), 0);
        Assert.assertEquals("float64Name1", float64Value1.getName());

        final FloatValue float64Value2 = (FloatValue) FloatValue.parserWith64Bit("float64Name2", Optional.empty())
                .parse(null, Unpooled.buffer(8).writeDouble(123.456));
        Assert.assertEquals(123.456, float64Value2.getValue(), 0);
        Assert.assertEquals("float64Name2", float64Value2.getName());

        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                4.9E-324,
                ((FloatValue) FloatValue.parserWith64Bit("float64Name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue(),
                0);
    }

    @Test
    public void testIPv4AddressValue() throws Exception {
        final IPv4AddressValue ipv4AddressValue =
                (IPv4AddressValue) IPv4AddressValue.parser("ipv4AddressName", Optional.empty())
                        .parse(
                                null,
                                Unpooled.wrappedBuffer(
                                        InetAddress.getByName("127.0.0.1").getAddress()));
        Assert.assertEquals("ipv4AddressName", ipv4AddressValue.getName());
        Assert.assertEquals("127.0.0.1", ipv4AddressValue.getValue().getHostAddress());
    }

    @Test
    public void testIPv6AddressValue() throws Exception {
        final IPv6AddressValue ipv6AddressValue =
                (IPv6AddressValue) IPv6AddressValue.parser("ipv6AddressName", Optional.empty())
                        .parse(
                                null,
                                Unpooled.wrappedBuffer(InetAddress.getByName("2001:638:301:11a0:d498:3253:ca5f:3777")
                                        .getAddress()));
        Assert.assertEquals("ipv6AddressName", ipv6AddressValue.getName());
        Assert.assertEquals(
                "2001:638:301:11a0:d498:3253:ca5f:3777",
                ipv6AddressValue.getValue().getHostAddress());
    }

    @Test
    public void testMacAddressValue() throws Exception {
        final MacAddressValue macAddressValue =
                (MacAddressValue) MacAddressValue.parser("macAddressName", Optional.empty())
                        .parse(null, Unpooled.wrappedBuffer(new byte[] {1, 2, 4, 8, 16, 32}));
        Assert.assertEquals("macAddressName", macAddressValue.getName());
        Assert.assertArrayEquals(new byte[] {1, 2, 4, 8, 16, 32}, macAddressValue.getValue());
    }

    @Test
    public void testOctetArrayValue() throws Exception {
        final OctetArrayValue octetArrayValue =
                (OctetArrayValue) OctetArrayValue.parser("octetArrayName", Optional.empty())
                        .parse(null, Unpooled.wrappedBuffer(new byte[] {1, 2, 4, 8, 16}));
        Assert.assertEquals("octetArrayName", octetArrayValue.getName());
        Assert.assertArrayEquals(new byte[] {1, 2, 4, 8, 16}, octetArrayValue.getValue());
    }

    @Test
    public void testSigned64Value() throws Exception {
        final SignedValue v1 = (SignedValue) SignedValue.parserWith64Bit("name1", Optional.empty())
                .parse(null, Unpooled.buffer(8).writeLong(0));
        Assert.assertEquals(0, v1.getValue(), 0);
        Assert.assertEquals("name1", v1.getName());

        final SignedValue v2 = (SignedValue) SignedValue.parserWith64Bit("name2", Optional.empty())
                .parse(null, Unpooled.buffer(8).writeLong(42));
        Assert.assertEquals(42, v2.getValue(), 0);
        Assert.assertEquals("name2", v2.getName());

        final SignedValue v3 = (SignedValue) SignedValue.parserWith64Bit("name3", Optional.empty())
                .parse(null, Unpooled.buffer(8).writeLong(-42));
        Assert.assertEquals(-42, v3.getValue(), 0);
        Assert.assertEquals("name3", v3.getName());

        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue(),
                0);
    }

    @Test
    public void testSigned32Value() throws Exception {
        final SignedValue v1 = (SignedValue) SignedValue.parserWith32Bit("name1", Optional.empty())
                .parse(null, Unpooled.buffer(4).writeInt(0));
        Assert.assertEquals(0, v1.getValue(), 0);
        Assert.assertEquals("name1", v1.getName());

        final SignedValue v2 = (SignedValue) SignedValue.parserWith32Bit("name2", Optional.empty())
                .parse(null, Unpooled.buffer(4).writeInt(42));
        Assert.assertEquals(42, v2.getValue(), 0);
        Assert.assertEquals("name2", v2.getName());

        final SignedValue v3 = (SignedValue) SignedValue.parserWith32Bit("name3", Optional.empty())
                .parse(null, Unpooled.buffer(4).writeInt(-42));
        Assert.assertEquals(-42, v3.getValue(), 0);
        Assert.assertEquals("name3", v3.getName());

        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue(),
                0);
    }

    @Test
    public void testSigned16Value() throws Exception {
        final SignedValue v1 = (SignedValue) SignedValue.parserWith16Bit("name1", Optional.empty())
                .parse(null, Unpooled.buffer(2).writeShort((short) 0));
        Assert.assertEquals(0, v1.getValue(), 0);
        Assert.assertEquals("name1", v1.getName());

        final SignedValue v2 = (SignedValue) SignedValue.parserWith16Bit("name2", Optional.empty())
                .parse(null, Unpooled.buffer(2).writeShort((short) 42));
        Assert.assertEquals(42, v2.getValue(), 0);
        Assert.assertEquals("name2", v2.getName());

        final SignedValue v3 = (SignedValue) SignedValue.parserWith16Bit("name3", Optional.empty())
                .parse(null, Unpooled.buffer(2).writeShort((short) -42));
        Assert.assertEquals(-42, v3.getValue(), 0);
        Assert.assertEquals("name3", v3.getName());

        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith16Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue(),
                0);
        Assert.assertEquals(
                1,
                ((SignedValue) SignedValue.parserWith16Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue(),
                0);
    }

    @Test
    public void testSigned8Value() throws Exception {
        final SignedValue v1 = (SignedValue) SignedValue.parserWith8Bit("name1", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer(new byte[] {0}));
        Assert.assertEquals(0, v1.getValue(), 0);
        Assert.assertEquals("name1", v1.getName());

        final SignedValue v2 = (SignedValue) SignedValue.parserWith8Bit("name2", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer(new byte[] {42}));
        Assert.assertEquals(42, v2.getValue(), 0);
        Assert.assertEquals("name2", v2.getName());

        final SignedValue v3 = (SignedValue) SignedValue.parserWith8Bit("name3", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer(new byte[] {-42}));
        Assert.assertEquals(-42, v3.getValue(), 0);
        Assert.assertEquals("name3", v3.getName());
    }

    @Test
    public void testUnsigned64Value() throws Exception {
        final UnsignedValue v1 = (UnsignedValue) UnsignedValue.parserWith64Bit("name1", Optional.empty())
                .parse(null, Unpooled.buffer(8).writeLong(0));
        Assert.assertEquals(0L, v1.getValue().longValue());
        Assert.assertEquals("name1", v1.getName());

        final UnsignedValue v2 = (UnsignedValue) UnsignedValue.parserWith64Bit("name2", Optional.empty())
                .parse(null, Unpooled.buffer(8).writeLong(42));
        Assert.assertEquals(42L, v2.getValue().longValue());
        Assert.assertEquals("name2", v2.getName());

        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith64Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue()
                        .longValue(),
                0);
    }

    @Test
    public void testUnsigned32Value() throws Exception {
        final UnsignedValue v1 = (UnsignedValue) UnsignedValue.parserWith32Bit("name1", Optional.empty())
                .parse(null, Unpooled.buffer(4).writeInt(0));
        Assert.assertEquals(0L, v1.getValue().longValue());
        Assert.assertEquals("name1", v1.getName());

        final UnsignedValue v2 = (UnsignedValue) UnsignedValue.parserWith32Bit("name2", Optional.empty())
                .parse(null, Unpooled.buffer(4).writeInt(42));
        Assert.assertEquals(42L, v2.getValue().longValue());
        Assert.assertEquals("name2", v2.getName());

        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith32Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue()
                        .longValue(),
                0);
    }

    @Test
    public void testUnsigned16Value() throws Exception {
        final UnsignedValue v1 = (UnsignedValue) UnsignedValue.parserWith16Bit("name1", Optional.empty())
                .parse(null, Unpooled.buffer(2).writeShort((short) 0));
        Assert.assertEquals(0L, v1.getValue().longValue());
        Assert.assertEquals("name1", v1.getName());

        final UnsignedValue v2 = (UnsignedValue) UnsignedValue.parserWith16Bit("name2", Optional.empty())
                .parse(null, Unpooled.buffer(2).writeShort((short) 42));
        Assert.assertEquals(42L, v2.getValue().longValue());
        Assert.assertEquals("name2", v2.getName());

        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith16Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {0, 1})))
                        .getValue()
                        .longValue(),
                0);
        Assert.assertEquals(
                1L,
                ((UnsignedValue) UnsignedValue.parserWith16Bit("name", Optional.empty())
                                .parse(null, Unpooled.wrappedBuffer(new byte[] {1})))
                        .getValue()
                        .longValue(),
                0);
    }

    @Test
    public void testUnsigned8Value() throws Exception {
        final UnsignedValue v1 = (UnsignedValue) UnsignedValue.parserWith8Bit("name1", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer(new byte[] {0}));
        Assert.assertEquals(0L, v1.getValue().longValue());
        Assert.assertEquals("name1", v1.getName());

        final UnsignedValue v2 = (UnsignedValue) UnsignedValue.parserWith8Bit("name2", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer(new byte[] {42}));
        Assert.assertEquals(42L, v2.getValue().longValue());
        Assert.assertEquals("name2", v2.getName());
    }

    @Test
    public void testStringValue() throws Exception {
        final StringValue v1 = (StringValue) StringValue.parser("name1", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer("Hello World".getBytes("UTF-8")));
        Assert.assertEquals("Hello World", v1.getValue());
        Assert.assertEquals("name1", v1.getName());

        final StringValue v2 = (StringValue) StringValue.parser("name2", Optional.empty())
                .parse(null, Unpooled.wrappedBuffer("Foo".getBytes("UTF-8")));
        Assert.assertEquals("Foo", v2.getValue());
        Assert.assertEquals("name2", v2.getName());

        final StringValue v3 = (StringValue)
                StringValue.parser("name3", Optional.empty()).parse(null, Unpooled.wrappedBuffer(new byte[] {}));
        Assert.assertEquals("", v3.getValue());
        Assert.assertEquals("name3", v3.getName());
    }
}
