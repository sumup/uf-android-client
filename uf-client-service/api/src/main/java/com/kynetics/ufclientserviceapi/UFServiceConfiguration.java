/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.ufclientserviceapi;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Daniele Sergio
 */
public class UFServiceConfiguration implements Serializable{

    public static class Builder {
        public Builder setTenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder setControllerId(String controllerId) {
            this.controllerId = controllerId;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setRetryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public UFServiceConfiguration build() {
            Objects.requireNonNull(tenant);
            Objects.requireNonNull(controllerId);
            Objects.requireNonNull(username);
            Objects.requireNonNull(password);
            Objects.requireNonNull(url);
            if(retryDelay < 0 ){
                throw new IllegalStateException("retryDelay must be grater than 0");
            }
            return new UFServiceConfiguration(tenant, controllerId, username, password, retryDelay, url);
        }

        private String tenant;
        private String controllerId;
        private String username;
        private String password;
        private long retryDelay;
        private String url;
    }

    public static Builder builder(){
        return new Builder();
    }

    public String getTenant() {
        return tenant;
    }

    public String getControllerId() {
        return controllerId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public String getUrl() {
        return url;
    }

    private UFServiceConfiguration(String tenant, String controllerId, String username, String password, long retryDelay, String url) {
        this.tenant = tenant;
        this.controllerId = controllerId;
        this.username = username;
        this.password = password;
        this.retryDelay = retryDelay;
        this.url = url;
    }

    private final String tenant;
    private final String controllerId;
    private final String username;
    private final String password;
    private final long retryDelay;
    private final String url;

    private static final long serialVersionUID = -8946542404771818353L;
}
