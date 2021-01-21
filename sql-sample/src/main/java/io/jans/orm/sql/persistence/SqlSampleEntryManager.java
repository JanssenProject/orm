/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.persistence;

import java.util.Properties;

import org.apache.log4j.Logger;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public class SqlSampleEntryManager {

    private static final Logger LOG = Logger.getLogger(SqlSampleEntryManager.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("sql.db.schema.name", "jans");
        connectionProperties.put("sql.connection.uri", "jdbc:mysql://localhost:3306/gluu");

        connectionProperties.put("sql.connection.driver-property.serverTimezone", "GMT+2");
        // Prefix connection.driver-property.key=value will be coverterd to key=value JDBC driver properties
        //connectionProperties.put("sql.connection.driver-property.driverProperty", "driverPropertyValue");

        connectionProperties.put("sql.auth.userName", "root");
        connectionProperties.put("sql.auth.userPassword", "Secret1!");
        
        // Password hash method
        connectionProperties.put("sql.password.encryption.method", "SSHA-256");
        
        // Connection pool size
        connectionProperties.put("sql.connection.pool.max-total", "5");
        connectionProperties.put("sql.connection.pool.max-idle", "3");
        connectionProperties.put("sql.connection.pool.min-idle", "2");
        
        // Max time needed to create connection pool in milliseconds
        connectionProperties.put("sql.connection.pool.create-max-wait-time-millis", "20000");
        
        // Max wait 20 seconds
        connectionProperties.put("sql.connection.pool.max-wait-time-millis", "20000");
        
        // Allow to evict connection in pool after 30 minutes
        connectionProperties.put("sql.connection.pool.min-evictable-idle-time-millis", "1800000");

        connectionProperties.put("sql.binaryAttributes", "objectGUID");
        connectionProperties.put("sql.certificateAttributes", "userCertificate");

        return connectionProperties;
    }

    public SqlEntryManager createSqlEntryManager() {
        SqlEntryManagerFactory sqlEntryManagerFactory = new SqlEntryManagerFactory();
        sqlEntryManagerFactory.create();
        Properties connectionProperties = getSampleConnectionProperties();

        SqlEntryManager sqlEntryManager = sqlEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created SqlEntryManager: " + sqlEntryManager);

        return sqlEntryManager;
    }

}