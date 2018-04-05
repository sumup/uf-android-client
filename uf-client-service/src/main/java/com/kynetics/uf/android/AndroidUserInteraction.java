/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android;

import com.kynetics.updatefactory.ddiclient.core.servicecallback.UserInteraction;

import java.util.concurrent.Future;

/**
 * @author Daniele Sergio
 */

public abstract class AndroidUserInteraction implements UserInteraction {
    private AuthorizationResponse auth;
    private Authorization authRequest;

    @Override
    public Future<Boolean> grantAuthorization(Authorization authorization) {
        auth = new AuthorizationResponse();
        authRequest = authorization;
        onAuthorizationAsked(authorization);
        return auth;
    }

    public void setAuthorization(Boolean authIsGranted){
        if(auth == null){
            return;
        }
        auth.put(authIsGranted);
        auth = null;
    }

    public Authorization getAuthRequest() {
        return authRequest;
    }

    protected abstract void onAuthorizationAsked(Authorization authorization);
}
