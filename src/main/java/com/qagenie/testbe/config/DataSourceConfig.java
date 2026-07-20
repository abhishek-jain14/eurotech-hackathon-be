package com.qagenie.testbe.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Switches the datasource based on qagenie.datasource.use-oracle-db:
 *   true (default)  - connects to the Oracle instance configured via
 *                      spring.datasource.* (url/username/password/driver,
 *                      pool sizing) in the active profile's
 *                      application-{profile}.yml. Schema must already
 *                      exist (see db-scripts/), hence ddl-auto=validate.
 *   false            - spins up an in-memory H2 database instead, with
 *                      Hibernate creating the schema straight from the
 *                      JPA entities. Handy for running/demoing locally
 *                      without a real Oracle instance.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "qagenie.datasource", name = "use-oracle-db", havingValue = "true", matchIfMissing = true)
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties oracleDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "qagenie.datasource", name = "use-oracle-db", havingValue = "true", matchIfMissing = true)
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource oracleDataSource(DataSourceProperties oracleDataSourceProperties) {
        return oracleDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "qagenie.datasource", name = "use-oracle-db", havingValue = "true", matchIfMissing = true)
    public HibernatePropertiesCustomizer oracleHibernatePropertiesCustomizer() {
        return properties -> properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
    }

    @Bean
    @ConditionalOnProperty(prefix = "qagenie.datasource", name = "use-oracle-db", havingValue = "false")
    public DataSource h2DataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:qagenie;DB_CLOSE_DELAY=-1;MODE=Oracle");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "qagenie.datasource", name = "use-oracle-db", havingValue = "false")
    public HibernatePropertiesCustomizer h2HibernatePropertiesCustomizer() {
        return properties -> {
            properties.put("hibernate.hbm2ddl.auto", "create-drop");
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        };
    }
}
