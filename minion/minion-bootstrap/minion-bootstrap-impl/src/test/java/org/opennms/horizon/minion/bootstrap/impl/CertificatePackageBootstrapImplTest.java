/*
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
 *
 */

package org.opennms.horizon.minion.bootstrap.impl;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class CertificatePackageBootstrapImplTest {

    private CertificatePackageBootstrapImpl target;

    private BiFunction<String, char[], ZipFile> mockZipFileFactory;
    private Function<String, File> mockFileFactory;
    private ZipFile mockZipFile;

    private FileHeader caCertFileHeader;
    private FileHeader clientCertFileHeader;
    private FileHeader clientKeyFileHeader;

    private List<FileHeader> testFileHeaderList;

    private File mockCaCertFile;
    private File mockClientCertFile;
    private File mockClientKeyFile;

    @BeforeEach
    public void setUp() throws ZipException {
        mockZipFileFactory = Mockito.mock(BiFunction.class);
        mockFileFactory = Mockito.mock(Function.class);
        mockZipFile = Mockito.mock(ZipFile.class);

        mockCaCertFile = Mockito.mock(File.class);
        mockClientCertFile = Mockito.mock(File.class);
        mockClientKeyFile = Mockito.mock(File.class);

        caCertFileHeader     = prepareFileHeader(CertificatePackageBootstrapImpl.CA_CERTIFICATE_ZIP_FILE_ENTRYNAME);
        clientCertFileHeader = prepareFileHeader(CertificatePackageBootstrapImpl.CLIENT_CERTIFICATE_ZIP_FILE_ENTRYNAME);
        clientKeyFileHeader  = prepareFileHeader(CertificatePackageBootstrapImpl.CLIENT_KEY_ZIP_FILE_ENTRYNAME);

        testFileHeaderList =
            List.of(
                prepareFileHeader(CertificatePackageBootstrapImpl.CA_CERTIFICATE_ZIP_FILE_ENTRYNAME),
                prepareFileHeader(CertificatePackageBootstrapImpl.CLIENT_CERTIFICATE_ZIP_FILE_ENTRYNAME),
                prepareFileHeader(CertificatePackageBootstrapImpl.CLIENT_KEY_ZIP_FILE_ENTRYNAME)
            );

        Mockito.when(mockZipFileFactory.apply("x-zip-path-x", null)).thenReturn(mockZipFile);
        Mockito.when(mockZipFile.getFileHeaders()).thenReturn(testFileHeaderList);

        Mockito.when(mockFileFactory.apply("x-ca-cert-output-path-x")).thenReturn(mockCaCertFile);
        Mockito.when(mockCaCertFile.getParent()).thenReturn("x-ca-cert-dir-x");
        Mockito.when(mockCaCertFile.getName()).thenReturn("x-ca-cert-filename-x");

        Mockito.when(mockFileFactory.apply("x-client-cert-output-path-x")).thenReturn(mockClientCertFile);
        Mockito.when(mockClientCertFile.getParent()).thenReturn("x-client-cert-dir-x");
        Mockito.when(mockClientCertFile.getName()).thenReturn("x-client-cert-filename-x");

        Mockito.when(mockFileFactory.apply("x-client-key-output-path-x")).thenReturn(mockClientKeyFile);
        Mockito.when(mockClientKeyFile.getParent()).thenReturn("x-client-key-dir-x");
        Mockito.when(mockClientKeyFile.getName()).thenReturn("x-client-key-filename-x");

        target = new CertificatePackageBootstrapImpl();

        target.setZipPath("x-zip-path-x");
        target.setZipFileFactory(mockZipFileFactory);
        target.setFileFactory(mockFileFactory);

        target.setCaCertOutputPath("x-ca-cert-output-path-x");
        target.setClientCertOutputPath("x-client-cert-output-path-x");
        target.setClientKeyOutputPath("x-client-key-output-path-x");

    }

    @Test
    void testValidExtractionNewFiles() throws ZipException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockZipFile.isValidZipFile()).thenReturn(true);

        Mockito.when(mockCaCertFile.exists()).thenReturn(false);
        Mockito.when(mockClientCertFile.exists()).thenReturn(false);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(false);

        //
        // Execute
        //
        target.init();

        //
        // Verify the Results
        //
        Mockito.verify(mockZipFile).extractFile(caCertFileHeader, "x-ca-cert-dir-x", "x-ca-cert-filename-x");
        Mockito.verify(mockZipFile).extractFile(clientCertFileHeader, "x-client-cert-dir-x", "x-client-cert-filename-x");
        Mockito.verify(mockZipFile).extractFile(clientKeyFileHeader, "x-client-key-dir-x", "x-client-key-filename-x");
    }

    @Test
    void testValidExtractionExistingFilesWithOverwite() throws ZipException {
        //
        // Setup Test Data and Interactions
        //

        Mockito.when(mockZipFile.isValidZipFile()).thenReturn(true);
        Mockito.when(mockCaCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(true);

        //
        // Execute
        //
        try (var logCaptor = LogCaptor.forClass(CertificatePackageBootstrapImpl.class)) {
            target.setOverwriteExisting(true);
            target.init();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                makeLogEventMatcher("Overwrite existing files enabled; not checking for existing file", "DEBUG", 0, null);

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            Mockito.verify(mockZipFile).extractFile(caCertFileHeader, "x-ca-cert-dir-x", "x-ca-cert-filename-x");
            Mockito.verify(mockZipFile).extractFile(clientCertFileHeader, "x-client-cert-dir-x", "x-client-cert-filename-x");
            Mockito.verify(mockZipFile).extractFile(clientKeyFileHeader, "x-client-key-dir-x", "x-client-key-filename-x");
        }
    }

    @Test
    void testValidExtractionExistingFilesWithoutOverwite() throws ZipException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockZipFile.isValidZipFile()).thenReturn(true);
        Mockito.when(mockZipFile.getFileHeaders()).thenReturn(testFileHeaderList);

        Mockito.when(mockCaCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(true);

        //
        // Execute
        //
        try (var logCaptor = LogCaptor.forClass(CertificatePackageBootstrapImpl.class)) {
            target.setOverwriteExisting(false);
            target.init();

            //
            // Verify the Results
            //
            var caCertLogMatcher = makeLogEventMatcher("Not overwriting existing file: destination={}", "WARN", 1, "x-ca-cert-output-path-x");
            var clientCertLogMatcher = makeLogEventMatcher("Not overwriting existing file: destination={}", "WARN", 1, "x-client-cert-output-path-x");
            var clientKeyLogMatcher = makeLogEventMatcher("Not overwriting existing file: destination={}", "WARN", 1, "x-client-key-output-path-x");
            assertTrue(logCaptor.getLogEvents().stream().anyMatch(caCertLogMatcher));
            assertTrue(logCaptor.getLogEvents().stream().anyMatch(clientCertLogMatcher));
            assertTrue(logCaptor.getLogEvents().stream().anyMatch(clientKeyLogMatcher));

            Mockito.verify(mockZipFile, Mockito.times(0)).extractFile(caCertFileHeader, "x-ca-cert-dir-x", "x-ca-cert-filename-x");
            Mockito.verify(mockZipFile, Mockito.times(0)).extractFile(clientCertFileHeader, "x-client-cert-dir-x", "x-client-cert-filename-x");
            Mockito.verify(mockZipFile, Mockito.times(0)).extractFile(clientKeyFileHeader, "x-client-key-dir-x", "x-client-key-filename-x");
        }
    }

    @Test
    void testWithNonNullPassword() throws ZipException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockZipFile.isValidZipFile()).thenReturn(true);

        Mockito.when(mockCaCertFile.exists()).thenReturn(false);
        Mockito.when(mockClientCertFile.exists()).thenReturn(false);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(false);

        String testPassword = "x-password-x";
        Mockito.reset(mockZipFileFactory);
        Mockito.when(mockZipFileFactory.apply("x-zip-path-x", testPassword.toCharArray())).thenReturn(mockZipFile);

        //
        // Execute
        //
        target.setPassword(testPassword);
        target.init();

        //
        // Verify the Results
        //
        Mockito.verify(mockZipFile).extractFile(caCertFileHeader, "x-ca-cert-dir-x", "x-ca-cert-filename-x");
        Mockito.verify(mockZipFile).extractFile(clientCertFileHeader, "x-client-cert-dir-x", "x-client-cert-filename-x");
        Mockito.verify(mockZipFile).extractFile(clientKeyFileHeader, "x-client-key-dir-x", "x-client-key-filename-x");
    }

    @Test
    void testInvalidZipFile() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockZipFile.isValidZipFile()).thenReturn(false);

        try (var logCaptor = LogCaptor.forClass(CertificatePackageBootstrapImpl.class)) {
            //
            // Execute
            //
            target.init();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                makeLogEventMatcher(
                    "Did not find a valid zip file to extract; ignoring: path={}",
                    "WARN",
                    1,
                    "x-zip-path-x");

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testExceptionOnExtract() throws ZipException {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");

        Mockito.when(mockZipFile.isValidZipFile()).thenReturn(true);
        Mockito.when(mockZipFile.getFileHeaders()).thenThrow(testException);

        try (var logCaptor = LogCaptor.forClass(CertificatePackageBootstrapImpl.class)) {
            //
            // Execute
            //
            Exception actualException = null;

            try {
                target.init();

                fail("missing expected exception");
            } catch (Exception exc) {
                actualException = exc;
            }

            //
            // Verify the Results
            //
            assertNotNull(actualException);
            assertEquals("Bootstrap process failed to extract the contents of the package file at x-zip-path-x", actualException.getMessage());
            assertSame(testException, actualException.getCause());

            Predicate<LogEvent> matcher =
                makeLogEventMatcher(
                    "Failed to extract contents of the certificate package zip file: path={}",
                    "ERROR",
                    1,
                    "x-zip-path-x");

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }

    }


    // TEST EXTRACT UNRECOGNIZED FILE ENTRY

    @Test
    void testExtractUnrecognizedFileEntry() throws ZipException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.reset(mockZipFile);
        Mockito.when(mockZipFile.isValidZipFile()).thenReturn(true);

        List<FileHeader> testInvalidEntryFileHeaderList =
            List.of(
                prepareFileHeader("x-invalid-entry-name-x")
            );

        Mockito.when(mockZipFile.getFileHeaders()).thenReturn(testInvalidEntryFileHeaderList);

        //
        // Execute
        //
        try (var logCaptor = LogCaptor.forClass(CertificatePackageBootstrapImpl.class)) {
            //
            // Execute
            //
            target.init();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                makeLogEventMatcher(
                    "Ignoring unexpected entry in certificate zip file: filename={}",
                    "WARN",
                    1,
                    "x-invalid-entry-name-x");

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));

            Mockito.verify(mockZipFile, Mockito.times(0)).extractFile(Mockito.anyString(), Mockito.anyString());
        }
    }


//========================================
// Internals
//----------------------------------------

    private FileHeader prepareFileHeader(String filename) {
        FileHeader fileHeader = new FileHeader();
        fileHeader.setFileName(filename);

        return fileHeader;
    }

    private Predicate<LogEvent> makeLogEventMatcher(String expectedMessage, String expectedLevel, int expectedNumArgument, String optionalExpectedArgument) {
        Predicate<LogEvent> matcher =
            (logEvent) ->
                (
                    Objects.equals(expectedMessage, logEvent.getMessage()) &&
                    Objects.equals(expectedLevel, logEvent.getLevel()) &&
                    (logEvent.getArguments().size() == expectedNumArgument) &&
                    (
                        (expectedNumArgument == 0) ||
                        Objects.equals(optionalExpectedArgument, logEvent.getArguments().get(0))
                    )
                );

        return matcher;
    }
}
