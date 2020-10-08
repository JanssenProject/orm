/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.ldap.model;

import java.io.Serializable;

import io.jans.persist.annotation.AttributeName;
import io.jans.persist.annotation.DN;
import io.jans.persist.annotation.DataEntry;
import io.jans.persist.annotation.ObjectClass;

/**
 * @author Yuriy Movchan
 * Date: 12/17/2019
 */
@DataEntry(configurationDefinition = true)
@ObjectClass(value = "ds-cfg-plugin")
public class MailUniquenessConfiguration implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;

    @DN
    private String dn;

    @AttributeName(name = "ds-cfg-enabled")
    private boolean enabled;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}