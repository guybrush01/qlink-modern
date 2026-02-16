/*
Copyright Jim Brain and Brain Innovations, 2005.

This file is part of QLinkServer.

QLinkServer is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

QLinkServer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with QLinkServer; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/

package org.jbrain.qlink.db.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Spring Data JPA Configuration for QLinkServer.
 * This configuration provides:
 * - DataSource using HikariCP connection pooling
 * - EntityManagerFactory with Hibernate JPA provider
 * - Transaction management support
 * - JPA repository scanning
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "org.jbrain.qlink.db.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class SpringDataJpaConfig {

    /**
     * Configuration properties for HikariCP.
     * Environment variables take precedence over default values.
     */
    private static final String JDBC_URL = System.getenv("QLINK_DB_JDBC_URI") != null
        ? System.getenv("QLINK_DB_JDBC_URI") : "jdbc:mysql://localhost:3306/qlink";
    private static final String JDBC_USERNAME = System.getenv("QLINK_DB_USERNAME") != null
        ? System.getenv("QLINK_DB_USERNAME") : "qlink";
    private static final String JDBC_PASSWORD = System.getenv("QLINK_DB_PASSWORD") != null
        ? System.getenv("QLINK_DB_PASSWORD") : "qlink";
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    /**
     * Creates and configures the HikariDataSource.
     *
     * @return configured DataSource
     */
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(JDBC_USERNAME);
        config.setPassword(JDBC_PASSWORD);
        config.setDriverClassName(JDBC_DRIVER);
        
        // Connection pool configuration
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Connection test configuration
        config.setConnectionTestQuery("SELECT 1");
        
        return new HikariDataSource(config);
    }

    /**
     * Creates and configures the EntityManagerFactory.
     *
     * @return configured EntityManagerFactory
     */
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource());
        factory.setPackagesToScan("org.jbrain.qlink.db.entity");
        
        // Use Hibernate as the JPA provider
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);
        
        // Hibernate configuration properties
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "false");
        properties.setProperty("hibernate.jdbc.batch_size", "50");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.query.fail_on_pagination_overfetching", "true");
        
        factory.setJpaProperties(properties);
        
        return factory;
    }

    /**
     * Creates and configures the TransactionManager.
     *
     * @return configured PlatformTransactionManager
     */
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }
}