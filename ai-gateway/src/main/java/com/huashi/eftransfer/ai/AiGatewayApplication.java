package com.huashi.eftransfer.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.huashi.eftransfer.ai")
public class AiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiGatewayApplication.class, args);
    }
}
