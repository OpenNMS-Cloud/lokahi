package org.opennms.horizon.exttest;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class TestMain implements CommandLineRunner {
    private static final Logger DEFAULT_LOGGER = org.slf4j.LoggerFactory.getLogger(TestMain.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    private ApplicationContext applicationContext;

//========================================
// Main
//----------------------------------------

    public static void main(String[] args) {
        TestMain instance = new TestMain();

        instance.instanceMain(args);
    }

    public void instanceMain(String[] args) {
        try {
            SpringApplication.run(TestMain.class, args);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

//========================================
// Command-Line
//----------------------------------------

    @Override
    public void run(String... args) throws Exception {
        CucumberCommandLineRunner runner = new CucumberCommandLineRunner();

        runner.run(args);
    }
}
