package com.nexus.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * 双数据源配置
 * @Primary MySQL — 供 MyBatis 使用
 * pgVectorDataSource — 仅供 PgVectorStore 使用
 */
@Configuration
public class VectorStoreConfig {

    // MySQL
    @Value("${spring.datasource.url}")
    private String mysqlUrl;
    @Value("${spring.datasource.username}")
    private String mysqlUsername;
    @Value("${spring.datasource.password}")
    private String mysqlPassword;

    // pgvector
    @Value("${spring.vector-datasource.url}")
    private String pgUrl;
    @Value("${spring.vector-datasource.username}")
    private String pgUsername;
    @Value("${spring.vector-datasource.password}")
    private String pgPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(mysqlUrl);
        ds.setUsername(mysqlUsername);
        ds.setPassword(mysqlPassword);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setMaximumPoolSize(20);
        ds.setMinimumIdle(5);
        return ds;
    }

    @Bean("pgVectorDataSource")
    @ConditionalOnProperty(prefix = "nexus.agent", name = "rag-enabled", havingValue = "true")
    public DataSource vectorDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(pgUrl);
        ds.setUsername(pgUsername);
        ds.setPassword(pgPassword);
        return ds;
    }

    @Bean
    @ConditionalOnProperty(prefix = "nexus.agent", name = "rag-enabled", havingValue = "true")
    public VectorStore vectorStore(@Qualifier("pgVectorDataSource") DataSource vectorDataSource, EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(vectorDataSource);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)
                .initializeSchema(true)
                .build();
    }
}
