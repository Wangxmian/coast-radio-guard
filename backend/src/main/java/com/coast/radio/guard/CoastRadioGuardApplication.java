package com.coast.radio.guard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.coast.radio.guard.mapper")
@SpringBootApplication
public class CoastRadioGuardApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoastRadioGuardApplication.class, args);
    }
}
