/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minioncertmanager.grpc;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.minioncertmanager.certificate.CaCertificateGenerator;
import org.opennms.horizon.minioncertmanager.certificate.PKCS12Generator;
import org.opennms.horizon.minioncertmanager.certificate.SerialNumberRepository;
import org.opennms.horizon.minioncertmanager.proto.EmptyResponse;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidRequest;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class MinionCertificateManagerImplTest {

    @Mock
    private PKCS12Generator pkcs12Generator;
    @Mock
    private X509Certificate certificate;

    @Mock
    private SerialNumberRepository serialNumberRepository;

    @TempDir()
    private File tempDir;
    private MinionCertificateManagerImpl minionCertificateManager;

    @BeforeEach
    public void setUp() throws Exception {
        CaCertificateGenerator.generate(tempDir, "OU=openNMS Test CA,C=CA", 3600);

        minionCertificateManager = new MinionCertificateManagerImpl(
            new File(tempDir, "ca.key"), new File(tempDir, "ca.crt"),
            pkcs12Generator, serialNumberRepository
        );

        lenient().when(certificate.getSerialNumber()).thenReturn(BigInteger.ONE);
        lenient().when(pkcs12Generator.generate(any(), any(), any(), any(), any(), any(), any())).thenReturn(certificate);
    }

    @Test
    public void requestCertificateWithEmptyDataFails() {
        createCertificate("", 0L, (response, error) -> {
            assertNull(response);
            assertNotNull(error);
        });
    }

    @Test
    public void requestCertificateWithInvalidDataFails() {
        createCertificate("\"; /dev/null", 50L, (response, error) -> {
            assertNull(response);
            assertNotNull(error);
        });
    }

    @Test
    public void requestCertificateWithValidDataProducesData() {
        String tenantId = "foo faz";
        Long location = 1010L;
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
    public void testMinionCertificateManagerImpl() {
        // Verify that the CA cert file is created
        File caCertFile = minionCertificateManager.getCaCertFile();
        assertTrue(caCertFile.exists());
    }

    @Test
    void testRevokeCertificate() throws RocksDBException, IOException {
        String tenantId = "foo faz";
        long location = 1010L;
        StreamObserver<EmptyResponse> observer = mock(StreamObserver.class);
        minionCertificateManager.revokeMinionCert(MinionCertificateRequest.newBuilder().setLocationId(location).setTenantId(tenantId).build(), observer);
        verify(serialNumberRepository, times(1))
            .revoke(tenantId, String.valueOf(location));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    void testSerialNumber() throws RocksDBException, IOException {
        String serial = "123456";

        StreamObserver<IsCertificateValidResponse> observer = mock(StreamObserver.class);
        minionCertificateManager.isCertValid( IsCertificateValidRequest.newBuilder().setSerialNumber(serial).build(), observer);
        verify(serialNumberRepository, times(1)).getBySerial(serial);
        verify(observer, times(1)).onCompleted();
    }

    private void createCertificate(String tenantId, Long locationId, BiConsumer<GetMinionCertificateResponse, Throwable> callback) {
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
            public void onCompleted() {

            }
        });
    }
}
