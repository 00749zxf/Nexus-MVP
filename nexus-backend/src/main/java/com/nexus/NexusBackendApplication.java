package com.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 应用启动类
 */
@SpringBootApplication
@EnableTransactionManagement
public class NexusBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusBackendApplication.class, args);
    }
}