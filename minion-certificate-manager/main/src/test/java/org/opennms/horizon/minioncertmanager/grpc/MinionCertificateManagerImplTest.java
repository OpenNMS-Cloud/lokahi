package org.opennms.horizon.minioncertmanager.grpc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.grpc.stub.StreamObserver;
import java.io.File;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.minioncertmanager.certificate.CaCertificateGenerator;
import org.opennms.horizon.minioncertmanager.certificate.PKCS12Generator;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateRequest;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;


@ExtendWith(MockitoExtension.class)
public class MinionCertificateManagerImplTest {

    @Mock
    private PKCS12Generator pkcs8Generator;

    @TempDir()
    private File tempDir;

    private MinionCertificateManagerImpl minionCertificateManager;

    @BeforeEach
    public void setUp() throws Exception {
        CaCertificateGenerator.generate(tempDir, "OU=openNMS Test CA,C=CA", 3600);

        minionCertificateManager = new MinionCertificateManagerImpl(
            new File(tempDir, "ca.key"), new File(tempDir, "ca.crt"),
            pkcs8Generator
        );
    }

    @Test
    public void requestCertificateWithEmptyDataFails() {
        createCertificate("", "", (response, error) -> {
            assertNull(response);
            assertNotNull(error);
        });
    }

    @Test
    public void requestCertificateWithInvalidDataFails() {
        createCertificate("\"; /dev/null", "&", (response, error) -> {
            assertNull(response);
            assertNotNull(error);
        });
    }

    @Test
    public void requestCertificateWithValidDataProducesData() {
        String tenantId = "foo faz";
        String location = "bar baz";
        createCertificate(tenantId, location, (response, error) -> {
            // validation of file existence - we still fail, but mocks should be called
            assertNull(response);
            assertNotNull(error);
            try {
                verify(pkcs8Generator).generate(eq(location), eq(tenantId), any(), any(), any(), any(), any());
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

    private void createCertificate(String tenantId, String location, BiConsumer<GetMinionCertificateResponse, Throwable> callback) {
        GetMinionCertificateRequest request = GetMinionCertificateRequest.newBuilder()
            .setTenantId(tenantId)
            .setLocation(location)
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
