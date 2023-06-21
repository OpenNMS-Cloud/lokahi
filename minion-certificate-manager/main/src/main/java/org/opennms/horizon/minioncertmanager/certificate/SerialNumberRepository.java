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

package org.opennms.horizon.minioncertmanager.certificate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

/**
 * DB structure is key (serial number) value (metadata)
 */
@Component
public class SerialNumberRepository {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(SerialNumberRepository.class);

    private final RocksDB db;
    public SerialNumberRepository(@Value("${grpc.server.db.url:/app/rocks-db}") String rootDir) throws RocksDBException{
        Objects.requireNonNull(rootDir);
        LOG.info("Beginning init of rocksDB: db-path={}", rootDir);
        try (Options dbOptions = new Options()) {
            dbOptions.setCreateMissingColumnFamilies(true)
                .setCreateIfMissing(true)
                .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
            ;
            this.db = RocksDB.open(dbOptions, rootDir);

            LOG.info("Successfully init rocksDB of {}", rootDir);
        }
    }


    public void close() {
        LOG.info("Begin to close rocketDb");
        if (db != null) {
            db.close();
        }
        LOG.info("Successfully closed rocketDb");
    }

    public void addCertificate(String tenantId, String locationId, X509Certificate certificate) throws RocksDBException, IOException {
        var meta = new CertificateMeta(tenantId, locationId, certificate);
        db.put(meta.getSerial().getBytes(), mapper.writeValueAsBytes(meta));
    }

    public void revoke(String tenantId, String locationId) throws RocksDBException, IOException {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(locationId);

        try (var ite = db.newIterator()) {
            ite.seekToFirst();
            while (ite.isValid()) {
                var meta = mapper.readValue(ite.value(), new TypeReference<CertificateMeta>() {
                });
                if (locationId.equals(meta.getLocationId()) && tenantId.equals(meta.getTenantId())) {
                    db.delete(ite.key());
                }
                ite.next();
            }
        }
    }

    public CertificateMeta getBySerial(String serial) throws IOException, RocksDBException {
        byte[] data = db.get(serial.getBytes());
        return mapper.readValue(data, new TypeReference<>() {
        });
    }

    public CertificateMeta getByLocationId(String tenantId, String locationId) throws IOException {
        Objects.requireNonNull(locationId);
        Objects.requireNonNull(tenantId);
        try (var ite = db.newIterator()) {
            ite.seekToFirst();
            while (ite.isValid()) {
                var meta = mapper.readValue(ite.value(), new TypeReference<CertificateMeta>() {
                });
                if (locationId.equals(meta.getLocationId()) && tenantId.equals(meta.getTenantId())) {
                    return meta;
                }
                ite.next();
            }
        }
        return null;
    }

    @NoArgsConstructor
    public static class CertificateMeta {
        @Getter
        private String serial;
        @Getter
        private String locationId;
        @Getter
        private String tenantId;
        @Getter
        private Date notBefore;
        @Getter
        private Date notAfter;

        public CertificateMeta(String tenantId, String locationId, X509Certificate certificate) {
            this.serial = certificate.getSerialNumber().toString(16).toUpperCase();
            this.locationId = locationId;
            this.tenantId = tenantId;
            this.notBefore = certificate.getNotBefore();
            this.notAfter = certificate.getNotAfter();
        }
    }
}
