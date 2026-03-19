package com.huashi.eftransfer.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.huashi.eftransfer.app")
@MapperScan(basePackages = "com.huashi.eftransfer.app.modules.system.mapper")
public class AppServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppServerApplication.class, args);
    }
}
