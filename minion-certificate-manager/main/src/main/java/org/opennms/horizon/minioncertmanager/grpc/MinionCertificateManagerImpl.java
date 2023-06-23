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

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.security.cert.CertificateException;
import java.util.regex.Pattern;
import org.opennms.horizon.minioncertmanager.certificate.CommandExecutor;
import org.opennms.horizon.minioncertmanager.certificate.PKCS12Generator;
import org.opennms.horizon.minioncertmanager.certificate.SerialNumberRepository;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;


@Component
public class MinionCertificateManagerImpl extends MinionCertificateManagerGrpc.MinionCertificateManagerImplBase {

    // permitted characters/values for tenant/location parameters
    private static final Pattern INPUT_PATTERN = Pattern.compile("[^a-zA-Z0-9_\\- ]");

    private static final Logger LOG = LoggerFactory.getLogger(MinionCertificateManagerImpl.class);
    private static final String FAILED_TO_GENERATE_ONE_OR_MORE_FILES = "Failed to generate one or more files.";
    public static final String CA_CERT_COMMAND = "openssl req -new -newkey rsa:4096 -days 3650 -nodes -x509 -subj \"/C=CA/ST=TBD/L=TBD/O=OpenNMS/CN=insecure-opennms-hs-ca\" -keyout \"%s\" -out \"%s\"";

    private final PKCS12Generator pkcs8Generator;
    private final File caCertFile;
    private final File caKeyFile;

    private final SerialNumberRepository serialNumberRepository;

    private CommandExecutor commandExecutor = new CommandExecutor();

    @Autowired
    public MinionCertificateManagerImpl(@Value("${manager.mtls.certificate}") File certificate,
        @Value("${manager.mtls.privateKey}") File privateKey, @Autowired SerialNumberRepository serialNumberRepository) throws IOException, InterruptedException {
        this(certificate, privateKey, new PKCS12Generator(), serialNumberRepository);
    }

    MinionCertificateManagerImpl(File certificate, File key, PKCS12Generator pkcs8Generator, SerialNumberRepository serialNumberRepository) throws IOException, InterruptedException {
        this.pkcs8Generator = pkcs8Generator;
        LOG.debug("=== TRYING TO RETRIEVE CA CERT");
        caCertFile = certificate;
        caKeyFile = key;

        if (!caCertFile.exists() || !caKeyFile.exists()) {
            LOG.warn("Generating new certificate and key");

            if (!caCertFile.exists() || !caKeyFile.exists()) {
                LOG.debug("=== GENERATE CA CERT");
                commandExecutor.executeCommand(CA_CERT_COMMAND, caKeyFile.getAbsolutePath(), caCertFile.getAbsolutePath());
            }
        }

        LOG.info("CA EXISTS: {}, CA PATH {}, CA CAN READ {}", caCertFile.exists(), caCertFile.getAbsolutePath(), caCertFile.canRead());
        this.serialNumberRepository = serialNumberRepository;
    }

    @Override
    public void getMinionCert(MinionCertificateRequest request, StreamObserver<GetMinionCertificateResponse> responseObserver) {
        Path tempDirectory = null;

        try {
            Long locationId = request.getLocationId();
            String tenantId = INPUT_PATTERN.matcher(request.getTenantId()).replaceAll("");

            if (locationId == 0L) {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Missing location and/or tenant information.").asException());
                return;
            }
            if (!tenantId.equals(request.getTenantId())) {
                // filtered values do not match input values, meaning we received invalid payload
                LOG.error("Received invalid input for certificate generation, locationId {}, tenant {}", locationId, request.getTenantId());
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Missing location and/or tenant information.").asException());
            }

            String password = UUID.randomUUID().toString();
            tempDirectory = Files.createTempDirectory(Files.createTempDirectory(Files.createTempDirectory("minioncert"), tenantId), String.valueOf(locationId));

            LOG.info("=== TEMP DIRECTORY: {}", tempDirectory.toAbsolutePath());
            LOG.info("exists: {}, isDirectory: {}, canRead: {}", tempDirectory.toFile().exists(), tempDirectory.toFile().isDirectory(), tempDirectory.toFile().canRead());
            File archive = new File(tempDirectory.toFile(), "minion.p12");

            // Generate PKCS8 files in the temporary directory
            var certificate = pkcs8Generator.generate(locationId, tenantId, tempDirectory, archive, password, caCertFile, caKeyFile);
            serialNumberRepository.addCertificate(tenantId, String.valueOf(locationId), certificate);

            if (!archive.exists()) {
                LOG.error(FAILED_TO_GENERATE_ONE_OR_MORE_FILES);
                responseObserver.onError(new RuntimeException(FAILED_TO_GENERATE_ONE_OR_MORE_FILES));
                return;
            }

            responseObserver.onNext(createResponse(Files.readAllBytes(archive.toPath()), password));
            responseObserver.onCompleted();
        } catch (IOException | InterruptedException | RocksDBException | CertificateException e) {
            LOG.error("Error while fetching certificate", e);
            responseObserver.onError(e);
        } finally {
            cleanFiles(tempDirectory);
        }
    }

    @Override
    public void getMinionCertMetadata(MinionCertificateRequest request, StreamObserver<GetMinionCertificateMetadataResponse> responseObserver) {
        try {
            var meta = serialNumberRepository.getByLocationId(request.getTenantId(), String.valueOf(request.getLocationId()));
            var response = GetMinionCertificateMetadataResponse.newBuilder()
                .setCreateDate(Timestamp.newBuilder().setSeconds(meta.getNotBefore().getTime()))
                .setExpireDate(Timestamp.newBuilder().setSeconds(meta.getNotAfter().getTime()))
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
        try {
            serialNumberRepository.revoke(request.getTenantId(), String.valueOf(request.getLocationId()));
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
    public void isCertValid(IsCertificateValidRequest request, StreamObserver<IsCertificateValidResponse> responseObserver) {
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
        LOG.info("CA EXISTS: {}, CA PATH: {}, CA CAN READ: {}, " +
                "CLIENT KEY EXISTS: {}, CLIENT KEY PATH: {}, " +
                "CLIENT KEY CAN READ: {}, CLIENT SIGNED CERT EXISTS: {}, " +
                "CLIENT SIGNED CERT PATH: {}, CLIENT SIGNED CERT CAN READ: {}",
            caCertFile.exists(), caCertFile.getAbsolutePath(), caCertFile.canRead(),
            clientKeyFile.exists(), clientKeyFile.getAbsolutePath(), clientKeyFile.canRead(),
            clientSignedCertFile.exists(), clientSignedCertFile.getAbsolutePath(), clientSignedCertFile.canRead());
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
                pathStream
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                LOG.error("Failed to clean up temporary directory", e);
            }
        }
    }

    public File getCaCertFile() {
        return caCertFile;
    }
}
