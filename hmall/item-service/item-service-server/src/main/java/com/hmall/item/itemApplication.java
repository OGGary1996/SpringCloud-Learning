package com.hmall.item;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@MapperScan("com.hmall.item.mapper")
@SpringBootApplication
@EnableDiscoveryClient
public class itemApplication {
    public static void main(String[] args) {
        SpringApplication.run(itemApplication.class, args);
    }
}