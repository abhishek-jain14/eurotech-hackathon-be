package com.qagenie.testbe.config;

import com.zaxxer.hikari.HikariDataSource;
import org.h2.server.web.JakartaWebServlet; // <--- MUST USE JakartaWebServlet
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

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

    // Despite the "oracle" naming (this bean predates the hackathon's DB target switching to
    // MySQL), this dialect just needs to match whatever spring.datasource.url actually points
    // at - see application-dev.properties for the real connection details.
    @Bean
    @ConditionalOnProperty(prefix = "qagenie.datasource", name = "use-oracle-db", havingValue = "true", matchIfMissing = true)
    public HibernatePropertiesCustomizer oracleHibernatePropertiesCustomizer() {
        return properties -> properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
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

    // Explicitly register JakartaWebServlet for H2 console
    @Bean
    @ConditionalOnProperty(prefix = "qagenie.datasource", name = "use-oracle-db", havingValue = "false")
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServletRegistration() {
        ServletRegistrationBean<JakartaWebServlet> registrationBean =
                new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }
}
