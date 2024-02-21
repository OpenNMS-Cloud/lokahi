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
package org.opennms.horizon.minioncertmanager.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.RocksDBException;

@ExtendWith(MockitoExtension.class)
class SerialNumberRepositoryTest {

    private SerialNumberRepository serialNumberRepository;

    @TempDir()
    private File tempDir;

    @Mock
    private X509Certificate certificate1;

    @Mock
    private X509Certificate certificate2;

    private final BigInteger serial1 = BigInteger.valueOf(12345L);
    private final BigInteger serial2 = BigInteger.valueOf(23456L);
    private final Date notBefore1 = new Date(System.currentTimeMillis());

    private final Date notBefore2 = new Date(notBefore1.getTime() - 1000_000L);

    // 1 year
    private final Date notAfter1 = new Date(System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L);
    private final Date notAfter2 = new Date(notAfter1.getTime() - 1000_000L);

    private final String tenantId1 = "tenantId1";
    private final String tenantId2 = "tenantId2";
    private final String locationId1 = "1";
    private final String locationId2 = "2";

    @BeforeEach
    public void setup() throws RocksDBException, IOException {
        serialNumberRepository = new SerialNumberRepository(tempDir.getAbsolutePath());
        lenient().when(certificate1.getSerialNumber()).thenReturn(serial1);
        lenient().when(certificate1.getNotBefore()).thenReturn(notBefore1);
        lenient().when(certificate1.getNotAfter()).thenReturn(notAfter1);
        lenient().when(certificate2.getSerialNumber()).thenReturn(serial2);
        lenient().when(certificate2.getNotBefore()).thenReturn(notBefore2);
        lenient().when(certificate2.getNotAfter()).thenReturn(notAfter2);

        serialNumberRepository.addCertificate(tenantId1, locationId1, certificate1);
        serialNumberRepository.addCertificate(tenantId2, locationId2, certificate2);
    }

    @Test
    void testGetBySerial() throws RocksDBException, IOException {
        var meta = serialNumberRepository.getBySerial(serial1.toString(16).toUpperCase());
        assertEquals(serial1.toString(16).toUpperCase(), meta.getSerial());
        assertEquals(notBefore1, meta.getNotBefore());
        assertEquals(notAfter1, meta.getNotAfter());
    }

    @Test
    void testGetByLocation() throws IOException {
        var meta = serialNumberRepository.getByLocationId(tenantId1, locationId1);
        assertEquals(serial1.toString(16).toUpperCase(), meta.getSerial());
        assertEquals(notBefore1, meta.getNotBefore());
        assertEquals(notAfter1, meta.getNotAfter());
    }

    @Test
    void testRevoke() throws RocksDBException, IOException {
        serialNumberRepository.revoke(tenantId1, locationId1);
        var meta = serialNumberRepository.getByLocationId(tenantId1, locationId1);
        assertNull(meta);
        var meta2 = serialNumberRepository.getByLocationId(tenantId2, locationId2);
        assertEquals(serial2.toString(16).toUpperCase(), meta2.getSerial());
        assertEquals(notBefore2, meta2.getNotBefore());
        assertEquals(notAfter2, meta2.getNotAfter());
    }

    @Test
    void testLoading() throws RocksDBException, IOException {
        serialNumberRepository.close();

        SerialNumberRepository newRepo = new SerialNumberRepository(tempDir.toString());
        var meta = newRepo.getByLocationId(tenantId1, locationId1);
        assertEquals(serial1.toString(16).toUpperCase(), meta.getSerial());
        assertEquals(notBefore1, meta.getNotBefore());
        assertEquals(notAfter1, meta.getNotAfter());
        var meta2 = newRepo.getByLocationId(tenantId2, locationId2);
        assertEquals(serial2.toString(16).toUpperCase(), meta2.getSerial());
        assertEquals(notBefore2, meta2.getNotBefore());
        assertEquals(notAfter2, meta2.getNotAfter());
    }
}
