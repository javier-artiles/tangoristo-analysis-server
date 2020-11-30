package com.tangoristo.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;


@Slf4j
@SpringBootApplication
@EnableScheduling
@ComponentScan("com.tangoristo.server")
@Profile(value = {"default", "local", "development", "production"})
public class TangoristoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TangoristoApplication.class, args);
    }

    // TODO do we really need this below?
    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void run() {
        log.info("Datasource {}", dataSource);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }
}
