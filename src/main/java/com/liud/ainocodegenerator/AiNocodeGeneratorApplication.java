package com.liud.ainocodegenerator;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.liud.ainocodegenerator.mapper")
@SpringBootApplication(exclude = RedisEmbeddingStoreAutoConfiguration.class)
public class AiNocodeGeneratorApplication {

    public static void main(String[] args) {
//        // 解决多个 HTTP 客户端冲突：指定使用 Spring Rest Client
//        System.setProperty("langchain4j.http.clientBuilderFactory",
//                "dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilderFactory");
        SpringApplication.run(AiNocodeGeneratorApplication.class, args);
        System.out.println("server finish start");
    }

}
