package com.nexus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 应用启动类
 */
@SpringBootApplication(exclude = {PgVectorStoreAutoConfiguration.class, DataSourceAutoConfiguration.class})
@EnableTransactionManagement
@MapperScan("com.nexus.mapper")
public class NexusBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusBackendApplication.class, args);
    }
}
