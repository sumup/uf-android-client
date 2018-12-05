/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.api;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * @author Daniele Sergio
 */
public class UFServiceConfiguration implements Serializable{

    public static class Builder {
        private Builder(){
            targetAttributes = new HashMap<>(2);
            targetAttributes.put("client","Android");
            final SimpleDateFormat sm = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z", Locale.getDefault());
            targetAttributes.put("date", sm.format(new Date()));

        }
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
            if(retryDelay >= 30_000) {
                this.retryDelay = retryDelay;
            }
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

        /**
         * @deprecated  As of release 0.3.4, replaced by {@link #withEnable(boolean)}
         */
        @Deprecated
        public Builder witEnable(boolean enable) {
            return withEnable(enable);
        }

        /**
         * @deprecated  As of release 0.3.4, replaced by {@link #withTargetAttributes(Map<String,String>)}
         */
        @Deprecated
        public Builder witArgs(Map<String,String> args) {
            return withTargetAttributes(args);
        }

        public Builder withEnable(boolean enable) {
            this.enable = enable;
            return this;
        }

        public Builder withTargetAttributes(Map<String,String> targetAttribute) {
            if(targetAttribute != null && targetAttribute.size() > 0) {
                this.targetAttributes = targetAttribute;
            }
            return this;
        }

        public Builder withIsUpdateFactoryServer(boolean isUpdateFactoryServer){
            this.isUpdateFactoryServer = isUpdateFactoryServer;
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
                    apiMode, enable, isUpdateFactoryServer,
                    targetAttributes);
        }

        public boolean configurationIsValid(){
            return notEmptyString(tenant)
                    && notEmptyString(controllerId)
                    && notEmptyString(url)
                    && retryDelay > 0;
        }

        private boolean notEmptyString(String stringToTest){
            return stringToTest != null && !stringToTest.isEmpty();
        }
        private String tenant;
        private String controllerId;
        private long retryDelay = 900_000;
        private String url;
        private boolean apiMode = true;
        private boolean enable = true;
        private boolean isUpdateFactoryServer = true;
        private String targetToken;
        private String gatewayToken;
        private Map<String,String> targetAttributes;
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

    public Boolean isEnable() {
        return enable;
    }

    public String getTargetToken() {
        return targetToken;
    }

    public String getGatewayToken() {
        return gatewayToken;
    }

    /**
     * @deprecated  As of release 0.3.4, replaced by {@link #getTargetAttributes()}
     */
    @Deprecated
    public Map<String, String> getArgs() {
        return getTargetAttributes();
    }

    public Map<String, String> getTargetAttributes() {
        return targetAttributes;
    }

    public boolean isUpdateFactoryServe() {
        return isUpdateFactoryServe;
    }

    private UFServiceConfiguration(String tenant,
                                   String controllerId,
                                   long retryDelay,
                                   String url,
                                   String targetToken,
                                   String gatewayToken,
                                   boolean apiMode,
                                   boolean isEnable,
                                   boolean isUpdateFactoryServe,
                                   Map<String,String> targetAttributes) {
        this.tenant = tenant;
        this.controllerId = controllerId;
        this.retryDelay = retryDelay;
        this.url = url;
        this.targetToken = targetToken;
        this.gatewayToken = gatewayToken;
        this.apiMode = apiMode;
        this.enable = isEnable;
        this.isUpdateFactoryServe = isUpdateFactoryServe;
        this.targetAttributes = targetAttributes;
    }

    private final String tenant;
    private final String controllerId;
    private final long retryDelay;
    private final String url;
    private final String targetToken;
    private final String gatewayToken;
    private final boolean apiMode;
    private final boolean enable;
    private final Map<String,String> targetAttributes;
    private final boolean isUpdateFactoryServe;

    private static final long serialVersionUID = -6025361892414738765L;
}
