package com.example.kbassistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.kbassistant.mapper")
public class KbAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbAssistantApplication.class, args);
    }
}
