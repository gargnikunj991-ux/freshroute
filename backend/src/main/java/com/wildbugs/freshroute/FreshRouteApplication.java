package com.wildbugs.freshroute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FreshRouteApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshRouteApplication.class, args);
    }
}

