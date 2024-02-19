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
package org.opennms.horizion.shared.utils;

import static org.junit.Assert.assertEquals;
import static org.opennms.horizon.shared.utils.InetAddressUtils.str;

import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.shared.utils.InetAddressUtils;

public class InetAddressUtilsTest {

    @Test
    public void testMacAddressFunctions() {
        byte[] expected = new byte[] {(byte) 0xff, (byte) 0x80, (byte) 0x0f, (byte) 0xf0, (byte) 0x01, (byte) 0x00};
        byte[] actual = InetAddressUtils.macAddressStringToBytes("ff:80:f:f0:01:00");
        Assert.assertArrayEquals(expected, actual);
        // assertEquals("FF:80:0F:F0:01:00", InetAddressUtils.macAddressBytesToString(actual));
        assertEquals("ff800ff00100", InetAddressUtils.macAddressBytesToString(actual));

        actual = InetAddressUtils.macAddressStringToBytes("ff:80:f:f0:01:0");
        Assert.assertArrayEquals(expected, actual);
        assertEquals("ff800ff00100", InetAddressUtils.macAddressBytesToString(actual));

        actual = InetAddressUtils.macAddressStringToBytes("ff800ff00100");
        Assert.assertArrayEquals(expected, actual);
        assertEquals("ff800ff00100", InetAddressUtils.macAddressBytesToString(actual));

        try {
            InetAddressUtils.macAddressStringToBytes("ff800ff0010");
            Assert.fail("Parsed MAC address value that was too short");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            InetAddressUtils.macAddressStringToBytes("ff800ff001000");
            Assert.fail("Parsed MAC address value that was too long");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testNMS4972() {
        String ip1 = "1.1.1.1";
        String ip2 = "255.255.255.255";
        Assert.assertFalse(BigInteger.ZERO.compareTo(InetAddressUtils.difference(ip1, ip2)) < 0);
    }

    @Test
    public void testCidrFunctions() {
        assertEquals("255.0.0.0", str(InetAddressUtils.convertCidrToInetAddressV4(8)));
        assertEquals("255.255.0.0", str(InetAddressUtils.convertCidrToInetAddressV4(16)));
        assertEquals("255.255.255.0", str(InetAddressUtils.convertCidrToInetAddressV4(24)));
        assertEquals("255.255.255.255", str(InetAddressUtils.convertCidrToInetAddressV4(32)));

        assertEquals("ffff:ffff:ffff:0000:0000:0000:0000:0000", str(InetAddressUtils.convertCidrToInetAddressV6(48)));
        assertEquals("ffff:ffff:ffff:ffff:0000:0000:0000:0000", str(InetAddressUtils.convertCidrToInetAddressV6(64)));
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", str(InetAddressUtils.convertCidrToInetAddressV6(128)));
    }
}
