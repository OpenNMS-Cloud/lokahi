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

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.minioncertmanager.certificate.CommandExecutor;
import org.opennms.horizon.minioncertmanager.certificate.PKCS12Generator;
import org.opennms.horizon.minioncertmanager.certificate.SerialNumberRepository;
import org.opennms.horizon.minioncertmanager.grpc.client.InventoryClient;
import org.opennms.horizon.minioncertmanager.proto.EmptyResponse;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateMetadataResponse;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidRequest;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateManagerGrpc;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MinionCertificateManagerImpl extends MinionCertificateManagerGrpc.MinionCertificateManagerImplBase {
    public static final String INVALID_LOCATION = "Invalid location.";

    // permitted characters/values for tenant/location parameters
    private static final Pattern INPUT_PATTERN = Pattern.compile("[^a-zA-Z0-9_\\- ]");

    private static final Logger LOG = LoggerFactory.getLogger(MinionCertificateManagerImpl.class);
    private static final String FAILED_TO_GENERATE_ONE_OR_MORE_FILES = "Failed to generate one or more files.";
    public static final String CA_CERT_COMMAND =
            "openssl req -new -newkey rsa:4096 -days 3650 -nodes -x509 -subj \"/C=CA/ST=TBD/L=TBD/O=OpenNMS/CN=insecure-opennms-hs-ca\" -keyout \"%s\" -out \"%s\"";

    private final PKCS12Generator pkcs8Generator;
    private final File caCertFile;
    private final File caKeyFile;

    private final SerialNumberRepository serialNumberRepository;

    private final InventoryClient inventoryClient;

    private CommandExecutor commandExecutor = new CommandExecutor();

    @Autowired
    public MinionCertificateManagerImpl(
            @Value("${manager.mtls.certificate}") File certificate,
            @Value("${manager.mtls.privateKey}") File privateKey,
            @Autowired SerialNumberRepository serialNumberRepository,
            @Autowired InventoryClient inventoryClient)
            throws IOException, InterruptedException {
        this(certificate, privateKey, new PKCS12Generator(), serialNumberRepository, inventoryClient);
    }

    MinionCertificateManagerImpl(
            File certificate,
            File key,
            PKCS12Generator pkcs8Generator,
            SerialNumberRepository serialNumberRepository,
            InventoryClient inventoryClient)
            throws IOException, InterruptedException {
        this.pkcs8Generator = pkcs8Generator;
        LOG.debug("=== TRYING TO RETRIEVE CA CERT");
        caCertFile = certificate;
        caKeyFile = key;

        if (!caCertFile.exists() || !caKeyFile.exists()) {
            LOG.warn("Generating new certificate and key");

            if (!caCertFile.exists() || !caKeyFile.exists()) {
                LOG.debug("=== GENERATE CA CERT");
                commandExecutor.executeCommand(
                        CA_CERT_COMMAND, caKeyFile.getAbsolutePath(), caCertFile.getAbsolutePath());
            }
        }

        LOG.info(
                "CA EXISTS: {}, CA PATH {}, CA CAN READ {}",
                caCertFile.exists(),
                caCertFile.getAbsolutePath(),
                caCertFile.canRead());
        this.serialNumberRepository = serialNumberRepository;
        this.inventoryClient = Objects.requireNonNull(inventoryClient);
    }

    private boolean locationExist(long locationId, String tenantId) {
        try {
            MonitoringLocationDTO locationDTO = inventoryClient.getLocationById(locationId, tenantId);
            return locationDTO != null;
        } catch (StatusRuntimeException ex) {
            LOG.error(String.format("Invalid location: %d tenantId: %s", locationId, tenantId));
            return false;
        }
    }

    @Override
    public void getMinionCert(
            MinionCertificateRequest request, StreamObserver<GetMinionCertificateResponse> responseObserver) {
        Path tempDirectory = null;

        try {
            Long locationId = request.getLocationId();
            String tenantId = INPUT_PATTERN.matcher(request.getTenantId()).replaceAll("");

            if (locationId == 0L) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Missing location and/or tenant information.")
                        .asException());
                return;
            }
            if (!locationExist(locationId, tenantId)) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(INVALID_LOCATION)
                        .asException());
                return;
            }
            if (!tenantId.equals(request.getTenantId())) {
                // filtered values do not match input values, meaning we received invalid payload
                LOG.error(
                        "Received invalid input for certificate generation, locationId {}, tenant {}",
                        locationId,
                        request.getTenantId());
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Missing location and/or tenant information.")
                        .asException());
            }

            String password = UUID.randomUUID().toString();
            tempDirectory = Files.createTempDirectory(
                    Files.createTempDirectory(Files.createTempDirectory("minioncert"), tenantId),
                    String.valueOf(locationId));

            LOG.info("=== TEMP DIRECTORY: {}", tempDirectory.toAbsolutePath());
            LOG.info(
                    "exists: {}, isDirectory: {}, canRead: {}",
                    tempDirectory.toFile().exists(),
                    tempDirectory.toFile().isDirectory(),
                    tempDirectory.toFile().canRead());
            File archive = new File(tempDirectory.toFile(), "minion.p12");

            // Generate PKCS8 files in the temporary directory
            var certificate = pkcs8Generator.generate(
                    locationId, tenantId, tempDirectory, archive, password, caCertFile, caKeyFile);
            serialNumberRepository.addCertificate(tenantId, String.valueOf(locationId), certificate);

            if (!archive.exists()) {
                LOG.error(FAILED_TO_GENERATE_ONE_OR_MORE_FILES);
                responseObserver.onError(new RuntimeException(FAILED_TO_GENERATE_ONE_OR_MORE_FILES));
                return;
            }

            responseObserver.onNext(createResponse(Files.readAllBytes(archive.toPath()), password));
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            LOG.error("InterruptedException while fetching certificate", e);
            Thread.currentThread().interrupt();
        } catch (IOException | RocksDBException | CertificateException e) {
            LOG.error("Error while fetching certificate", e);
            responseObserver.onError(e);
        } finally {
            cleanFiles(tempDirectory);
        }
    }

    @Override
    public void getMinionCertMetadata(
            MinionCertificateRequest request, StreamObserver<GetMinionCertificateMetadataResponse> responseObserver) {
        if (!locationExist(request.getLocationId(), request.getTenantId())) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(INVALID_LOCATION).asException());
            return;
        }
        try {
            var meta = serialNumberRepository.getByLocationId(
                    request.getTenantId(), String.valueOf(request.getLocationId()));
            var response = GetMinionCertificateMetadataResponse.newBuilder()
                    .setCreateDate(Timestamp.newBuilder()
                            .setSeconds(meta.getNotBefore().getTime()))
                    .setExpireDate(
                            Timestamp.newBuilder().setSeconds(meta.getNotAfter().getTime()))
                    .setSerialNumber(meta.getSerial());
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IOException e) {
            LOG.error("Fail to get Minion Certificate Metadata. Request: {} Error: {}", request, e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void revokeMinionCert(MinionCertificateRequest request, StreamObserver<EmptyResponse> responseObserver) {
        if (!locationExist(request.getLocationId(), request.getTenantId())) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(INVALID_LOCATION).asException());
            return;
        }
        try {
            serialNumberRepository.revoke(request.getTenantId(), String.valueOf(request.getLocationId()));
            responseObserver.onNext(EmptyResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (RocksDBException | IOException e) {
            LOG.error("Fail to revoke minion cert for {}. Error: {}", request, e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteMinionCert(MinionCertificateRequest request, StreamObserver<EmptyResponse> responseObserver) {
        this.revokeMinionCert(request, responseObserver);
    }

    @Override
    public void isCertValid(
            IsCertificateValidRequest request, StreamObserver<IsCertificateValidResponse> responseObserver) {
        try {
            var meta = serialNumberRepository.getBySerial(request.getSerialNumber());
            var response = IsCertificateValidResponse.newBuilder().setIsValid(meta != null);
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IOException | RocksDBException e) {
            LOG.error("Fail to validate minion cert for {}. Error: {}", request, e.getMessage());
            responseObserver.onError(e);
        }
    }

    private boolean validatePKCS8Files(File directory) {
        File clientKeyFile = new File(directory, "client.key");
        File clientSignedCertFile = new File(directory, "client.signed.cert");
        LOG.info(
                "CA EXISTS: {}, CA PATH: {}, CA CAN READ: {}, " + "CLIENT KEY EXISTS: {}, CLIENT KEY PATH: {}, "
                        + "CLIENT KEY CAN READ: {}, CLIENT SIGNED CERT EXISTS: {}, "
                        + "CLIENT SIGNED CERT PATH: {}, CLIENT SIGNED CERT CAN READ: {}",
                caCertFile.exists(),
                caCertFile.getAbsolutePath(),
                caCertFile.canRead(),
                clientKeyFile.exists(),
                clientKeyFile.getAbsolutePath(),
                clientKeyFile.canRead(),
                clientSignedCertFile.exists(),
                clientSignedCertFile.getAbsolutePath(),
                clientSignedCertFile.canRead());
        return caCertFile.exists() && clientKeyFile.exists() && clientSignedCertFile.exists();
    }

    private GetMinionCertificateResponse createResponse(byte[] zipBytes, String password) {
        return GetMinionCertificateResponse.newBuilder()
                .setCertificate(ByteString.copyFrom(zipBytes))
                .setPassword(password)
                .build();
    }

    private void cleanFiles(Path tempDirectory) {
        // Clean up the temporary directory and its contents
        if (tempDirectory != null) {
            try (Stream<Path> pathStream = Files.walk(tempDirectory)) {
                pathStream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                LOG.error("Failed to clean up temporary directory", e);
            }
        }
    }

    public File getCaCertFile() {
        return caCertFile;
    }
}
