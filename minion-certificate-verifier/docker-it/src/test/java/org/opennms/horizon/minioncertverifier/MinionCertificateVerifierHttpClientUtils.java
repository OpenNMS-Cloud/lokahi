package org.opennms.horizon.minioncertverifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionCertificateVerifierHttpClientUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MinionCertificateVerifierHttpClientUtils.class);

    private static final String LOCALHOST = "localhost";

    private URI uri;
    private Builder clientBuilder;

    public void externalHttpPortInSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        int port = Integer.parseInt(value);
        this.clientBuilder = HttpRequest.newBuilder();
        this.uri = URI.create("http://" + LOCALHOST + ":" + port + "/certificate/debug");
        LOG.info("Using external service address {}", uri);
    }

    public CompletableFuture<Map<String, List<String>>> validateCertificateData(String certificateDn) {
        HttpRequest rq = clientBuilder.uri(uri).GET()
            .header("ssl-client-subject-dn", certificateDn)
            .build();
        return HttpClient.newHttpClient()
            .sendAsync(rq, BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalArgumentException("Result failed to pass validation rules");
                }
                return response.headers().map();
            })
            .whenComplete((result, error) -> {
                if (error != null) {
                    LOG.warn("Error while awaiting service answer", error);
                    return;
                }
                LOG.info("Received service headers {}", result);
            });
    }


}
