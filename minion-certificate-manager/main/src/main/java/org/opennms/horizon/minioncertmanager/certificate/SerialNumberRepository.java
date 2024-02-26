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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;
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

/**
 * DB structure is key (serial number) value (metadata)
 */
@Component
public class SerialNumberRepository {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(SerialNumberRepository.class);

    private final RocksDB db;

    public SerialNumberRepository(@Value("${grpc.server.db.url:/app/rocks-db}") String rootDir)
            throws RocksDBException {
        Objects.requireNonNull(rootDir);
        LOG.info("Beginning init of rocksDB: db-path={}", rootDir);
        try (Options dbOptions = new Options()) {
            dbOptions
                    .setCreateMissingColumnFamilies(true)
                    .setCreateIfMissing(true)
                    .setCompressionType(CompressionType.SNAPPY_COMPRESSION);
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

    public void addCertificate(String tenantId, String locationId, X509Certificate certificate)
            throws RocksDBException, IOException {
        var meta = new CertificateMeta(tenantId, locationId, certificate);
        db.put(meta.getSerial().getBytes(), mapper.writeValueAsBytes(meta));
    }

    public void revoke(String tenantId, String locationId) throws RocksDBException, IOException {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(locationId);

        try (var ite = db.newIterator()) {
            ite.seekToFirst();
            while (ite.isValid()) {
                var meta = mapper.readValue(ite.value(), new TypeReference<CertificateMeta>() {});
                if (locationId.equals(meta.getLocationId()) && tenantId.equals(meta.getTenantId())) {
                    db.delete(ite.key());
                }
                ite.next();
            }
        }
    }

    public CertificateMeta getBySerial(String serial) throws IOException, RocksDBException {
        byte[] data = db.get(serial.getBytes());
        return (data == null) ? null : mapper.readValue(data, new TypeReference<>() {});
    }

    public CertificateMeta getByLocationId(String tenantId, String locationId) throws IOException {
        Objects.requireNonNull(locationId);
        Objects.requireNonNull(tenantId);
        try (var ite = db.newIterator()) {
            ite.seekToFirst();
            while (ite.isValid()) {
                var meta = mapper.readValue(ite.value(), new TypeReference<CertificateMeta>() {});
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
