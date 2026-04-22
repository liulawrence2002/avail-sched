package com.goblinscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GoblinSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoblinSchedulerApplication.class, args);
    }
}
