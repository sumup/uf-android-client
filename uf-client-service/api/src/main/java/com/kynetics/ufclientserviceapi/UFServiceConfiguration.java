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
        public Builder withTenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder withControllerId(String controllerId) {
            this.controllerId = controllerId;
            return this;
        }

        public Builder withRetryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withApiMode(boolean apiMode) {
            this.apiMode = apiMode;
            return this;
        }

        public Builder witEnable(boolean enable) {
            this.enable = enable;
            return this;
        }

        public UFServiceConfiguration build() {
            Objects.requireNonNull(tenant);
            Objects.requireNonNull(controllerId);
            Objects.requireNonNull(url);
            if(retryDelay < 0 ){
                throw new IllegalStateException("retryDelay must be grater than 0");
            }
            return new UFServiceConfiguration(tenant, controllerId, retryDelay, url, apiMode, enable);
        }

        private String tenant;
        private String controllerId;
        private long retryDelay;
        private String url;
        private boolean apiMode = true;
        private boolean enable = true;
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

    public long getRetryDelay() {
        return retryDelay;
    }

    public String getUrl() {
        return url;
    }

    public Boolean isApiMode() {
        return apiMode;
    }

    public Boolean isEnalbe() {
        return enalbe;
    }

    public UFServiceConfiguration(String tenant, String controllerId, long retryDelay, String url, boolean apiMode, boolean isEnable) {
        this.tenant = tenant;
        this.controllerId = controllerId;
        this.retryDelay = retryDelay;
        this.url = url;
        this.apiMode = apiMode;
        this.enalbe = isEnable;
    }

    private final String tenant;
    private final String controllerId;
    private final long retryDelay;
    private final String url;
    private final Boolean apiMode;
    private final Boolean enalbe;

    private static final long serialVersionUID = -7212858346341036166L;
}
