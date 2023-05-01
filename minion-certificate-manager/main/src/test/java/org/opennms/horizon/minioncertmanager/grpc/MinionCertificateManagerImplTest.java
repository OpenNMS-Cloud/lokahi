package org.opennms.horizon.minioncertmanager.grpc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.minioncertmanager.certificate.CertFileUtils;
import org.opennms.horizon.minioncertmanager.certificate.PKCS8Generator;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(MockitoExtension.class)
public class MinionCertificateManagerImplTest {

    @InjectMocks
    private MinionCertificateManagerImpl minionCertificateManager;

    @Mock
    private PKCS8Generator pkcs8Generator;

    @Mock
    private CertFileUtils certFileUtils;

    @AfterAll
    public static void cleanUp() {
        File caCertFile = new File("ca.cert");
        File caKeyFile = new File("ca.key");
        if (caCertFile.exists()) {
            caCertFile.delete();
        }
        if(caKeyFile.exists()) {
            caKeyFile.delete();
        }
    }

    @Test
    public void testMinionCertificateManagerImpl() {
        // Verify that the CA cert file is created
        File caCertFile = minionCertificateManager.getCaCertFile();
        assertTrue(caCertFile.exists());
    }
}
