package com.liud.ainocodegenerator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.liud.ainocodegenerator.mapper")
@SpringBootApplication
public class AiNocodeGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiNocodeGeneratorApplication.class, args);
        System.out.println("server finish start");
    }

}
