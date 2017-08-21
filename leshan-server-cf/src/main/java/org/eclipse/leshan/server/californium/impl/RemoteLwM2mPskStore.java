/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.Arrays;

import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RemoteRegistrationStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteLwM2mPskStore implements PskStore {

    private SecurityStore securityStore;
    private RemoteRegistrationStore registrationStore;
    private static final Logger LOG = LoggerFactory.getLogger(RemoteLwM2mPskStore.class);

    public RemoteLwM2mPskStore(SecurityStore securityStore) {
        this(securityStore, null);
    }

    public RemoteLwM2mPskStore(SecurityStore securityStore, RemoteRegistrationStore registrationStore) {
        this.securityStore = securityStore;
        this.registrationStore = registrationStore;
    }

    @Override
    public byte[] getKey(String identity) {
        SecurityInfo info = securityStore.getByIdentity(identity);
        if (info == null || info.getPreSharedKey() == null) {
            return null;
        } else {
            // defensive copy
            return Arrays.copyOf(info.getPreSharedKey(), info.getPreSharedKey().length);
        }
    }

    @Override
    public byte[] getKey(ServerNames serverNames, String identity) {
        // serverNames is not supported
        return getKey(identity);
    }

    @Override
    public String getIdentity(InetSocketAddress inetAddress) {
        if (registrationStore == null)
            return null;

        Registration registration = null;
        try {
            registration = registrationStore.getRegistrationByAdress(inetAddress);
        } catch (RemoteException e) {
            LOG.error("Failed to get registration by address via RMI", e);
        }
        if (registration != null) {
            SecurityInfo securityInfo = securityStore.getByEndpoint(registration.getEndpoint());
            if (securityInfo != null) {
                return securityInfo.getIdentity();
            }
            return null;
        }
        return null;
    }
}
