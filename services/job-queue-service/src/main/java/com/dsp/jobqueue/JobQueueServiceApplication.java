package com.dsp.jobqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobQueueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobQueueServiceApplication.class, args);
    }
}
