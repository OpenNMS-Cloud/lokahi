package org.opennms.horizon.minioncertmanager.certificate;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPackager {
    public static File createZipPackage(String locationName, String password, File certFile, Path tempDirectory) throws IOException {
        File zipFile = new File(tempDirectory + File.separator + "minion.zip");

        String minionName = "Minion1";
        if (locationName != null && !locationName.isBlank()) {
            minionName += "-" + locationName;
        }
        FileOutputStream fout = new FileOutputStream(zipFile.getPath());
        ZipOutputStream zipout = new ZipOutputStream(fout);

        ZipEntry zipEntry = new ZipEntry("storage/minion.p12");
        zipout.putNextEntry(zipEntry);
        zipout.write(Files.readAllBytes(certFile.toPath()));

        InputStream dockerStream = ZipPackager.class.getClassLoader().getResourceAsStream("run-minion-docker-compose.yaml");
        if (dockerStream == null) {
            throw new IOException("Unable to stream docker compose resource");
        }
        String dockerTxt = new BufferedReader(new InputStreamReader(dockerStream)).lines()
                .parallel().collect(Collectors.joining("\n"));
        dockerTxt = dockerTxt.replace("[KEYSTORE_PASSWORD]", password);
        dockerTxt = dockerTxt.replace("[MINION_NAME]", minionName);
        zipEntry = new ZipEntry("docker-compose.yaml");
        zipout.putNextEntry(zipEntry);
        zipout.write(dockerTxt.getBytes());

        zipout.close();

        return zipFile;
    }
}
