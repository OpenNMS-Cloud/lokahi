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

import java.util.Enumeration;
import java.util.List;

import org.opennms.horizon.minioncertverifier.parser.CertificateDnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final Logger logger = LoggerFactory.getLogger(CertificateController.class);
    private final String headerName;
    private final CertificateDnParser certificateDnParser;

    public CertificateController(@Value("${certificate.header.name:ssl-client-subject-dn}") String headerName, CertificateDnParser certificateDnParser) {
        this.headerName = headerName;
        this.certificateDnParser = certificateDnParser;
    }

    @GetMapping("/debug")
    public ResponseEntity<Void> validateDebug(HttpServletRequest request) throws Exception {
        Enumeration<String> headerNames = request.getHeaderNames();

        logger.info("Received headers: ");
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            logger.info("\t{}: {}", header, request.getHeader(header));
        }

        String subjectDn = request.getHeader(headerName);
        return validate(subjectDn);
    }

    @GetMapping
    public ResponseEntity<Void> validate(@RequestHeader("ssl-client-subject-dn") String clientSubjectDn) throws Exception {
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

        var span = Span.current();
        if (span.isRecording()) {
            span.setAttribute("ssl-client-subject-dn", clientSubjectDn);
            span.setAttribute("user", tenant);
            span.setAttribute("location", location);
        }

        if (tenant.isBlank() || location.isBlank() || !location.matches("^\\d+$")) {
            span.setStatus(StatusCode.ERROR);
            return ResponseEntity.notFound().build();
        }

        span.setStatus(StatusCode.OK);
        return ResponseEntity.ok()
            .header("tenant-id", tenant)
            .header("location-id", location)
            .build();
    }

}
