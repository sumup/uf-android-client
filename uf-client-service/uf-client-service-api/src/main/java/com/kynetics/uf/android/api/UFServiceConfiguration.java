/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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

        public Builder withGetawayToken(String gatewayToken) {
            this.gatewayToken = gatewayToken;
            return this;
        }

        public Builder withTargetToken(String targetToken) {
            this.targetToken = targetToken;
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

        public Builder witArgs(Map<String,String> args) {
            this.args = args;
            return this;
        }


        public UFServiceConfiguration build() {
            Objects.requireNonNull(tenant);
            Objects.requireNonNull(controllerId);
            Objects.requireNonNull(url);
            if(retryDelay < 0 ){
                throw new IllegalStateException("retryDelay must be grater than 0");
            }
            return new UFServiceConfiguration(tenant, controllerId, retryDelay, url,
                    targetToken == null ? "" : targetToken,
                    gatewayToken == null ? "" : gatewayToken,
                    apiMode, enable, args != null ? args : new HashMap<>(0));
        }

        private String tenant;
        private String controllerId;
        private long retryDelay;
        private String url;
        private boolean apiMode = true;
        private boolean enable = true;
        private String targetToken;
        private String gatewayToken;
        private Map<String,String> args;
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
        return enable;
    }

    public String getTargetToken() {
        return targetToken;
    }

    public String getGatewayToken() {
        return gatewayToken;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    private UFServiceConfiguration(String tenant,
                                   String controllerId,
                                   long retryDelay,
                                   String url,
                                   String targetToken,
                                   String gatewayToken,
                                   boolean apiMode,
                                   boolean isEnable,
                                   Map<String,String> args) {
        this.tenant = tenant;
        this.controllerId = controllerId;
        this.retryDelay = retryDelay;
        this.url = url;
        this.targetToken = targetToken;
        this.gatewayToken = gatewayToken;
        this.apiMode = apiMode;
        this.enable = isEnable;
        this.args = args;
    }

    private final String tenant;
    private final String controllerId;
    private final long retryDelay;
    private final String url;
    private final String targetToken;
    private final String gatewayToken;
    private final Boolean apiMode;
    private final Boolean enable;
    private final Map<String,String> args;

    private static final long serialVersionUID = -2015373437277468594L;
}
