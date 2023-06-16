package org.opennms.horizon.exttest;

import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.runtime.Runtime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CucumberCommandLineRunner {
    public static final String[] DEFAULT_CUCUMBER_OPTIONS = {
            "--plugin", "json:cucumber.reports/cucumber-report.json",
            "--plugin", "html:cucumber.reports/cucumber-report.html",
            "--plugin", "pretty",
            "--glue", "org.opennms.horizon.it",
            "classpath:org/opennms/horizon/it"
    };
    @Autowired
    private ApplicationContext applicationContext;

    public void run() {
        CommandlineOptionsParser commandlineOptionsParser = new CommandlineOptionsParser(System.out);

        RuntimeOptions runtimeOptions =
                commandlineOptionsParser.parse(DEFAULT_CUCUMBER_OPTIONS)
                .build();

        Runtime cucumberRuntime =
                Runtime.builder()
                        .withRuntimeOptions(runtimeOptions)
                        .withClassLoader(() -> this.getClass().getClassLoader())
                        .build()
                ;

        cucumberRuntime.run();

        System.exit(cucumberRuntime.exitStatus());
    }
}
