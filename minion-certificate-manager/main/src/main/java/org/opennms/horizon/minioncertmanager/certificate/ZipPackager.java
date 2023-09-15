package org.opennms.horizon.minioncertmanager.certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPackager {
    private static final Logger LOG = LoggerFactory.getLogger(ZipPackager.class);

    public static File createZipPackage(Long locationId, String password, File certFile, Path tempDirectory) throws IOException {
        File zipFile = new File(tempDirectory + File.separator + "minion.zip");

        FileOutputStream fout = new FileOutputStream(zipFile.getPath());
        ZipOutputStream zipout = new ZipOutputStream(fout);

        ZipEntry zipEntry = new ZipEntry("storage/minion.p12");
        zipout.putNextEntry(zipEntry);
        zipout.write(Files.readAllBytes(certFile.toPath()));

        InputStream dockerStream = ZipPackager.class.getClassLoader().getResourceAsStream("run-minion-docker-compose.yaml");
        String dockerTxt = new BufferedReader(new InputStreamReader(dockerStream)).lines()
                .parallel().collect(Collectors.joining("\n"));
        dockerTxt = dockerTxt.replace("[KEYSTORE_PASSWORD]", password);
        zipEntry = new ZipEntry("docker-compose.yaml");
        zipout.putNextEntry(zipEntry);
        zipout.write(dockerTxt.getBytes());

        zipout.close();

        return zipFile;
    }
}
