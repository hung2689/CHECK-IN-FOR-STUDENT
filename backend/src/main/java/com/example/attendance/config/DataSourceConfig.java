package com.example.attendance.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (!StringUtils.hasText(rawUrl)) {
            rawUrl = System.getenv("DATABASE_URL");
        }
        if (!StringUtils.hasText(rawUrl)) {
            rawUrl = properties.getUrl();
        }

        HikariDataSource dataSource = new HikariDataSource();

        if (StringUtils.hasText(rawUrl)) {
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(rawUrl.replace("postgres://", "http://").replace("postgresql://", "http://"));
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String query = uri.getQuery();

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    if (StringUtils.hasText(query)) {
                        jdbcUrl += "?" + query;
                    }

                    dataSource.setJdbcUrl(jdbcUrl);

                    if (uri.getUserInfo() != null) {
                        String[] parts = uri.getUserInfo().split(":", 2);
                        dataSource.setUsername(parts[0]);
                        if (parts.length > 1) {
                            dataSource.setPassword(parts[1]);
                        }
                    }
                    log.info("Configured HikariDataSource from Cloud DATABASE_URL for host: {}", host);
                    return dataSource;
                } catch (URISyntaxException e) {
                    log.warn("Failed to parse URI DATABASE_URL, fallback to standard properties", e);
                }
            } else if (rawUrl.startsWith("jdbc:")) {
                dataSource.setJdbcUrl(rawUrl);
                dataSource.setUsername(properties.getUsername());
                dataSource.setPassword(properties.getPassword());
                log.info("Configured HikariDataSource from standard JDBC URL");
                return dataSource;
            }
        }

        log.warn("DATABASE_URL is not set or defaulted to local. Connecting using properties: {}", properties.getUrl());
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return dataSource;
    }
}
