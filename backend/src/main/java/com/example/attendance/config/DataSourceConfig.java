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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    private static final Pattern USERINFO_PATTERN = Pattern.compile("^(?:jdbc:)?postgres(?:ql)?://([^:]+):([^@]+)@([^/]+)(/.*)?$");

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
        String driver = StringUtils.hasText(properties.getDriverClassName())
                ? properties.getDriverClassName()
                : "org.postgresql.Driver";
        dataSource.setDriverClassName(driver);

        if (StringUtils.hasText(rawUrl)) {
            Matcher matcher = USERINFO_PATTERN.matcher(rawUrl);
            if (matcher.matches()) {
                String user = matcher.group(1);
                String pass = matcher.group(2);
                String host = matcher.group(3);
                String path = matcher.group(4) != null ? matcher.group(4) : "";

                String jdbcUrl = "jdbc:postgresql://" + host + path;
                dataSource.setJdbcUrl(jdbcUrl);
                dataSource.setUsername(user);
                dataSource.setPassword(pass);
                log.info("Configured HikariDataSource from credentials URL for host: {}", host);
                return dataSource;
            }

            // Standard JDBC URL without embedded credentials
            String jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:" + rawUrl;
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : System.getenv("SPRING_DATASOURCE_USERNAME"));
            dataSource.setPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : System.getenv("SPRING_DATASOURCE_PASSWORD"));
            log.info("Configured HikariDataSource from standard JDBC URL");
            return dataSource;
        }

        log.warn("Using default DataSourceProperties: {}", properties.getUrl());
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return dataSource;
    }
}
