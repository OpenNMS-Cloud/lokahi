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

package org.opennms.horizon.minioncertverifier.controller;

import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.List;

import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertverifier.parser.CertificateDnParser;
import org.opennms.horizon.minioncertverifier.parser.CertificateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/certificate")
public class CertificateController {
    public static final String TENANTID_KEY = "tenant-id";
    public static final String LOCATIONID_KEY = "location-id";

    public static final String ERROR_HEADER_KEY = "error";

    private final Logger logger = LoggerFactory.getLogger(CertificateController.class);
    private final CertificateDnParser certificateDnParser;

    private final MinionCertificateManagerClient minionCertificateManagerClient;

    public CertificateController(CertificateDnParser certificateDnParser,
                                 MinionCertificateManagerClient minionCertificateManagerClient) {
        this.certificateDnParser = certificateDnParser;
        this.minionCertificateManagerClient = minionCertificateManagerClient;
    }

    @GetMapping("/debug")
    public ResponseEntity<Void> validateDebug(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();

        logger.info("Received headers: ");
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            logger.info("\t{}: {}", header, request.getHeader(header));
        }

        String cert = request.getHeader("ssl-client-cert");
        return validate(cert);
    }

    @GetMapping
    public ResponseEntity<Void> validate(@RequestHeader("ssl-client-cert") String certificatePem) {
        var span = Span.current();
        CertificateParser parser;
        try {
            parser = new CertificateParser(certificatePem);
            span.setAttribute("serial-number", parser.getSerialNumber());
        } catch (Exception ex) {
            // we want to capture all exceptions include null pointer
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        IsCertificateValidResponse response = minionCertificateManagerClient.isCertValid(parser.getSerialNumber());
        if (!response.getIsValid()) {
            span.setStatus(StatusCode.ERROR, "certificate manager reported that certificate is invalid");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var clientSubjectDn = parser.getSubjectDn();
        List<String> values = certificateDnParser.get(clientSubjectDn, "OU");
        String tenant = "";
        String location = "";

        for (String value : values) {
            if (value.startsWith("T:")) {
                tenant = value.substring(2);
            }
            if (value.startsWith("L:")) {
                location = value.substring(2);
            }
        }


        if (span.isRecording()) {
            span.setAttribute("ssl-client-subject-dn", clientSubjectDn);
            span.setAttribute("user", tenant);
            span.setAttribute("location", location);
        }

        if (tenant.isBlank()) {
            span.setStatus(StatusCode.ERROR, TENANTID_KEY + " is blank");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(ERROR_HEADER_KEY, "MISSING " + TENANTID_KEY).build();
        }
        if (location.isBlank()) {
            span.setStatus(StatusCode.ERROR, LOCATIONID_KEY + " is blank");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(ERROR_HEADER_KEY, "MISSING " + LOCATIONID_KEY).build();
        }
        if (!location.matches("^\\d+$")) {
            span.setStatus(StatusCode.ERROR, LOCATIONID_KEY + " is not number");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(ERROR_HEADER_KEY, "INVALID " + LOCATIONID_KEY).build();
        }

        span.setStatus(StatusCode.OK);
        return ResponseEntity.ok()
            .header(TENANTID_KEY, tenant)
            .header(LOCATIONID_KEY, location)
            .build();
    }
}
