package org.opennms.horizon.datachoices.config;

import liquibase.integration.spring.SpringLiquibase;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.Properties;

@Configuration
public class QuartzConfig {
    private final QuartzProperties quartzProperties;

    private final ApplicationContext applicationContext;

    public QuartzConfig(QuartzProperties quartzProperties,
                        ApplicationContext applicationContext,
                        // Injecting SpringLiquibase as it is a dependency to Quartz
                        SpringLiquibase springLiquibase) {
        this.quartzProperties = quartzProperties;
        this.applicationContext = applicationContext;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerJobFactory jobFactory = new SchedulerJobFactory();
        jobFactory.setApplicationContext(applicationContext);

        Properties properties = new Properties();
        properties.putAll(quartzProperties.getProperties());

        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(properties);
        factory.setJobFactory(jobFactory);
        return factory;
    }

    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) {
        return schedulerFactoryBean.getScheduler();
    }
}
