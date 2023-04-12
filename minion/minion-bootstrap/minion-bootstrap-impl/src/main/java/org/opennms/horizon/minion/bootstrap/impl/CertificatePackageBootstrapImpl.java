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
import org.opennms.horizon.minion.bootstrap.CertificatePackageBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CertificatePackageBootstrapImpl implements CertificatePackageBootstrap {

    public static final String CA_CERTIFICATE_ZIP_FILE_ENTRYNAME = "CA.cert";
    public static final String CLIENT_CERTIFICATE_ZIP_FILE_ENTRYNAME = "client.signed.cert";
    public static final String CLIENT_KEY_ZIP_FILE_ENTRYNAME = "client.key";

    private static final Logger LOG = LoggerFactory.getLogger(CertificatePackageBootstrapImpl.class);

    private BiFunction<String, char[], ZipFile> zipFileFactory = ZipFile::new;
    private Function<String, File> fileFactory = File::new;

    private boolean overwriteExisting;
    private String password;
    private String zipPath;

    private String clientKeyOutputPath;
    private String clientCertOutputPath;
    private String caCertOutputPath;

//========================================
// Getters and Setters
//----------------------------------------

    public BiFunction<String, char[], ZipFile> getZipFileFactory() {
        return zipFileFactory;
    }

    public void setZipFileFactory(BiFunction<String, char[], ZipFile> zipFileFactory) {
        this.zipFileFactory = zipFileFactory;
    }

    public Function<String, File> getFileFactory() {
        return fileFactory;
    }

    public void setFileFactory(Function<String, File> fileFactory) {
        this.fileFactory = fileFactory;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getZipPath() {
        return zipPath;
    }

    public void setZipPath(String zipPath) {
        this.zipPath = zipPath;
    }

    public String getClientKeyOutputPath() {
        return clientKeyOutputPath;
    }

    public void setClientKeyOutputPath(String clientKeyOutputPath) {
        this.clientKeyOutputPath = clientKeyOutputPath;
    }

    public String getClientCertOutputPath() {
        return clientCertOutputPath;
    }

    public void setClientCertOutputPath(String clientCertOutputPath) {
        this.clientCertOutputPath = clientCertOutputPath;
    }

    public String getCaCertOutputPath() {
        return caCertOutputPath;
    }

    public void setCaCertOutputPath(String caCertOutputPath) {
        this.caCertOutputPath = caCertOutputPath;
    }

//========================================
// Lifecycle
//----------------------------------------

    public void init() {
        try ( ZipFile zipFile = zipFileFactory.apply(zipPath, safePasswordToCharArray()) ) {
            if (zipFile.isValidZipFile()) {
                extractCertificateFiles(zipFile);
            } else {
                LOG.warn("Did not find a valid zip file to extract; ignoring: path={}", zipPath);
            }
        } catch (Exception exc) {
            LOG.error("Failed to extract contents of the certificate package zip file: path={}", zipPath, exc);
            throw new RuntimeException("Bootstrap process failed to extract the contents of the package file at " + zipPath, exc);
        }
    }

//========================================
// Internals
//----------------------------------------

    private char[] safePasswordToCharArray() {
        if (password == null) {
            return null;
        }

        return password.toCharArray();
    }

    private void extractCertificateFiles(ZipFile zipFile) throws ZipException {
        List<FileHeader> fileHeaders = zipFile.getFileHeaders();

        for (FileHeader oneFileHeader : fileHeaders ) {
            String filename = oneFileHeader.getFileName();
            switch (filename) {
                case CertificatePackageBootstrapImpl.CA_CERTIFICATE_ZIP_FILE_ENTRYNAME -> processCaCertificateEntry(zipFile, oneFileHeader);
                case CertificatePackageBootstrapImpl.CLIENT_CERTIFICATE_ZIP_FILE_ENTRYNAME -> processClientCertificateEntry(zipFile, oneFileHeader);
                case CertificatePackageBootstrapImpl.CLIENT_KEY_ZIP_FILE_ENTRYNAME -> processClientKeyEntry(zipFile, oneFileHeader);
                default -> LOG.warn("Ignoring unexpected entry in certificate zip file: filename={}", filename);
            }
        }
    }

    private void processCaCertificateEntry(ZipFile zipFile, FileHeader fileHeader) throws ZipException {
        LOG.info("Extracting the CA Certificate entry: output-path={}", caCertOutputPath);
        commonProcessFileEntry(zipFile, fileHeader, caCertOutputPath);
    }

    private void processClientCertificateEntry(ZipFile zipFile, FileHeader fileHeader) throws ZipException {
        LOG.info("Extracting the Client Certificate entry: output-path={}", clientCertOutputPath);
        commonProcessFileEntry(zipFile, fileHeader, clientCertOutputPath);
    }

    private void processClientKeyEntry(ZipFile zipFile, FileHeader fileHeader) throws ZipException {
        LOG.info("Extracting the Client Key entry: output-path={}", clientKeyOutputPath);
        commonProcessFileEntry(zipFile, fileHeader, clientKeyOutputPath);
    }

    private void commonProcessFileEntry(ZipFile zipFile, FileHeader fileHeader, String destinationPath) throws ZipException {
        File destinationFile = fileFactory.apply(destinationPath);

        if (overwriteExisting) {
            LOG.debug("Overwrite existing files enabled; not checking for existing file");
        } else {
            if (destinationFile.exists()) {
                LOG.warn("Not overwriting existing file: destination={}", destinationPath);
                return;
            }
        }

        //
        // Split out the directory name and filename.  The extractFile() method requires them to be separated.
        //
        String dir = destinationFile.getParent();
        String basename = destinationFile.getName();
        zipFile.extractFile(fileHeader, dir, basename);
    }
}
