package com.formcoach;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.formcoach.mapper")
public class FormCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormCoachApplication.class, args);
    }
}
