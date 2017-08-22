/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.server.RemoteEndpoint;
import org.eclipse.leshan.server.RemoteMessageInterceptor;
import org.eclipse.leshan.server.impl.ProxyMessageInterceptor;

public class RemoteEndpointImpl implements RemoteEndpoint {

    private Endpoint endpoint;

    private List<ProxyMessageInterceptor> interceptors = new ArrayList<ProxyMessageInterceptor>();

    public RemoteEndpointImpl(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void addInterceptor(RemoteMessageInterceptor remoteInterceptor) throws RemoteException {
        ProxyMessageInterceptor interceptor = new ProxyMessageInterceptor(remoteInterceptor);
        this.interceptors.add(interceptor);
        this.endpoint.addInterceptor(interceptor);
    }

    @Override
    public void removeInterceptor(RemoteMessageInterceptor remoteInterceptor) {
        ProxyMessageInterceptor toDelete = null;
        for (ProxyMessageInterceptor p : this.interceptors) {
            if (p.getRemteMessageInterceptor() == remoteInterceptor) {
                this.endpoint.removeInterceptor(p);
                toDelete = p;
            }
        }
        if (toDelete != null) {
            this.interceptors.remove(toDelete);
        }
    }
}
