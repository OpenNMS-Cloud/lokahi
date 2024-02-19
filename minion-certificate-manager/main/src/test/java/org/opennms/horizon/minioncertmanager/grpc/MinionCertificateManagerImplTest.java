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
package org.opennms.horizon.minioncertmanager.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.minioncertmanager.certificate.CaCertificateGenerator;
import org.opennms.horizon.minioncertmanager.certificate.PKCS12Generator;
import org.opennms.horizon.minioncertmanager.certificate.SerialNumberRepository;
import org.opennms.horizon.minioncertmanager.grpc.client.InventoryClient;
import org.opennms.horizon.minioncertmanager.proto.EmptyResponse;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidRequest;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.rocksdb.RocksDBException;

@ExtendWith(MockitoExtension.class)
class MinionCertificateManagerImplTest {

    @Mock
    private PKCS12Generator pkcs12Generator;

    @Mock
    private X509Certificate certificate;

    @Mock
    private SerialNumberRepository serialNumberRepository;

    @Mock
    private InventoryClient inventoryClient;

    @TempDir()
    private File tempDir;

    private MinionCertificateManagerImpl minionCertificateManager;

    @BeforeEach
    public void setUp() throws Exception {
        CaCertificateGenerator.generate(tempDir, "OU=openNMS Test CA,C=CA", 3600);

        minionCertificateManager = new MinionCertificateManagerImpl(
                new File(tempDir, "ca.key"),
                new File(tempDir, "ca.crt"),
                pkcs12Generator,
                serialNumberRepository,
                inventoryClient);

        lenient().when(certificate.getSerialNumber()).thenReturn(BigInteger.ONE);
        lenient()
                .when(pkcs12Generator.generate(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(certificate);
    }

    @Test
    void requestCertificateWithEmptyDataFails() {
        createCertificate("", 0L, (response, error) -> {
            assertNull(response);
            assertNotNull(error);
        });
    }

    @Test
    void requestCertificateWithInvalidDataFails() {
        createCertificate("\"; /dev/null", 50L, (response, error) -> {
            assertNull(response);
            assertNotNull(error);
        });
    }

    @Test
    void requestCertificateWithValidDataProducesData() {
        String tenantId = "foo faz";
        Long location = 1010L;
        when(inventoryClient.getLocationById(location, tenantId))
                .thenReturn(MonitoringLocationDTO.newBuilder()
                        .setLocation(String.valueOf(location))
                        .build());
        createCertificate(tenantId, location, (response, error) -> {
            // validation of file existence - we still fail, but mocks should be called
            assertNull(response);
            assertNotNull(error);
            try {
                verify(pkcs12Generator).generate(eq(location), eq(tenantId), any(), any(), any(), any(), any());
                verify(serialNumberRepository, times(1))
                        .addCertificate(eq(tenantId), eq(String.valueOf(location)), any(X509Certificate.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void requestCertificateWithInValidLocation() {
        String tenantId = "foo faz";
        Long location = 1010L;

        createCertificate(tenantId, location, (response, error) -> {
            // validation of file existence - we still fail, but mocks should be called
            assertNull(response);
            assertNotNull(error);
            StatusException statusRuntimeException = (StatusException) error;
            assertEquals(
                    Status.INVALID_ARGUMENT.getCode().value(),
                    statusRuntimeException.getStatus().getCode().value());
            assertEquals(
                    MinionCertificateManagerImpl.INVALID_LOCATION,
                    statusRuntimeException.getStatus().getDescription());
        });
    }

    @Test
    void testMinionCertificateManagerImpl() {
        // Verify that the CA cert file is created
        File caCertFile = minionCertificateManager.getCaCertFile();
        assertTrue(caCertFile.exists());
    }

    @Test
    void testRevokeCertificate() throws RocksDBException, IOException {
        String tenantId = "foo faz";
        long location = 1010L;
        when(inventoryClient.getLocationById(location, tenantId))
                .thenReturn(MonitoringLocationDTO.newBuilder()
                        .setLocation(String.valueOf(location))
                        .build());
        StreamObserver<EmptyResponse> observer = mock(StreamObserver.class);
        minionCertificateManager.revokeMinionCert(
                MinionCertificateRequest.newBuilder()
                        .setLocationId(location)
                        .setTenantId(tenantId)
                        .build(),
                observer);
        verify(serialNumberRepository, times(1)).revoke(tenantId, String.valueOf(location));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    void testSerialNumber() throws RocksDBException, IOException {
        String serial = "123456";

        StreamObserver<IsCertificateValidResponse> observer = mock(StreamObserver.class);
        minionCertificateManager.isCertValid(
                IsCertificateValidRequest.newBuilder().setSerialNumber(serial).build(), observer);
        verify(serialNumberRepository, times(1)).getBySerial(serial);
        verify(observer, times(1)).onCompleted();
    }

    private void createCertificate(
            String tenantId, Long locationId, BiConsumer<GetMinionCertificateResponse, Throwable> callback) {
        MinionCertificateRequest request = MinionCertificateRequest.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(locationId)
                .build();

        minionCertificateManager.getMinionCert(request, new StreamObserver<>() {
            @Override
            public void onNext(GetMinionCertificateResponse value) {
                callback.accept(value, null);
            }

            @Override
            public void onError(Throwable t) {
                callback.accept(null, t);
            }

            @Override
            public void onCompleted() {}
        });
    }
}
