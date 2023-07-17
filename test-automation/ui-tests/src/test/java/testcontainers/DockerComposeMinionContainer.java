package testcontainers;

import org.opennms.horizon.systemtests.CucumberHooks;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.io.File;
import java.time.Duration;
import static org.opennms.horizon.systemtests.CucumberHooks.DC_MINIONS;


public class DockerComposeMinionContainer extends DockerComposeContainer<DockerComposeMinionContainer> {

    private DockerComposeMinionContainer(String dockerComposeYamlFile) {
        super(new File(dockerComposeYamlFile));
    }

    public static void createNewContainer(String dockerComposeYamlFile, File cert, String minionId, String password) {
        DockerComposeMinionContainer dockerComposeMinionContainer = new DockerComposeMinionContainer(dockerComposeYamlFile);

        dockerComposeMinionContainer.withEnv("MINION_ID", minionId)
            .withEnv("MINION_GATEWAY_HOST", CucumberHooks.gatewayUrl)
            .withEnv("MINION_GATEWAY_PORT", CucumberHooks.gatewayPort)
            .withEnv("GRPC_CLIENT_KEYSTORE_PASSWORD", password)
            .withEnv("CERT_FOLDER", cert.getAbsolutePath())
            .withLocalCompose(true)
            .withLogConsumer("minion-tenanta",
                new Slf4jLogConsumer(LoggerFactory.getLogger(DockerComposeMinionContainer.class))
            )
            .withLogConsumer("snmpd-temp2",
                new Slf4jLogConsumer(LoggerFactory.getLogger(DockerComposeMinionContainer.class))
            )
            .waitingFor("minion-tenanta",
                Wait.forLogMessage(".* Udp Flow Listener started at .*", 3)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )
            .waitingFor("minion-tenanta",
                Wait.forLogMessage(".*Sending heartbeat from Minion.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(2))
            )
            .waitingFor("snmpd-temp2",
                Wait.forLogMessage(".*.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(2))
            );


        DC_MINIONS.put(minionId, dockerComposeMinionContainer);
        dockerComposeMinionContainer.start();
    }

}
