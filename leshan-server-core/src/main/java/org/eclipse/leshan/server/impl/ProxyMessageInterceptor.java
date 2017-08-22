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
package org.eclipse.leshan.server.impl;

import java.rmi.RemoteException;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.eclipse.leshan.server.RemoteMessageInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyMessageInterceptor implements MessageInterceptor {
    private final RemoteMessageInterceptor remoteInterceptor;
    private static final Logger LOG = LoggerFactory.getLogger(ProxyMessageInterceptor.class);

    public ProxyMessageInterceptor(RemoteMessageInterceptor remoteInterceptor) {
        this.remoteInterceptor = remoteInterceptor;
    }

    public RemoteMessageInterceptor getRemteMessageInterceptor() {
        return this.remoteInterceptor;
    }

    @Override
    public void sendRequest(Request request) {
        try {
            remoteInterceptor.sendRequest(request);
        } catch (RemoteException e) {
            LOG.error("Failed to send request via RMI", e);
        }
    }

    @Override
    public void sendResponse(Response response) {
        try {
            remoteInterceptor.sendResponse(response);
        } catch (RemoteException e) {
            LOG.error("Failed to send response via RMI", e);
        }
    }

    @Override
    public void sendEmptyMessage(EmptyMessage message) {
        try {
            remoteInterceptor.sendEmptyMessage(message);
        } catch (RemoteException e) {
            LOG.error("Failed to send empty message via RMI", e);
        }
    }

    @Override
    public void receiveRequest(Request request) {
        try {
            remoteInterceptor.receiveRequest(request);
        } catch (RemoteException e) {
            LOG.error("Failed to receive request via RMI", e);
        }
    }

    @Override
    public void receiveResponse(Response response) {
        try {
            remoteInterceptor.receiveResponse(response);
        } catch (RemoteException e) {
            LOG.error("Failed to receive response via RMI", e);
        }
    }

    @Override
    public void receiveEmptyMessage(EmptyMessage message) {
        try {
            remoteInterceptor.receiveEmptyMessage(message);
        } catch (RemoteException e) {
            LOG.error("Failed to receive empty message via RMI", e);
        }
    }

}
