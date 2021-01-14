/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.Properties;

import org.apache.log4j.Logger;
import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;

/**
 * @author Yuriy Movchan
 * Date: 01/13/2017
 */
public class CouchbaseSampleEntryManager {

    private static final Logger LOG = Logger.getLogger(CouchbaseSampleEntryManager.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("couchbase.servers", "test.jans.info");
        connectionProperties.put("couchbase.auth.userName", "admin");
        connectionProperties.put("couchbase.auth.userPassword", "secret");
//        connectionProperties.put("couchbase.buckets", "jans");
        connectionProperties.put("couchbase.buckets", "jans, jans_user, jans_token");

        connectionProperties.put("couchbase.bucket.default", "jans");
        connectionProperties.put("couchbase.bucket.jans_user.mapping", "people, groups");
        connectionProperties.put("couchbase.bucket.jans_token.mapping", "sessions");

        connectionProperties.put("couchbase.password.encryption.method", "CRYPT-SHA-256");

        return connectionProperties;
    }

    public CouchbaseEntryManager createCouchbaseEntryManager() {
        CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
        couchbaseEntryManagerFactory.create();
        Properties connectionProperties = getSampleConnectionProperties();

        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created CouchbaseEntryManager: " + couchbaseEntryManager);

        return couchbaseEntryManager;
    }

}