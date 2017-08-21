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
package org.eclipse.leshan.server.demo;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.eclipse.leshan.server.californium.RemoteCaliforniumRegistrationStore;

public interface RemoteConfiguration extends Remote {
    static final String LOOKUPNAME = "RemoteConfiguration";

    void setRegistrationStore(RemoteCaliforniumRegistrationStore remoteRegistrationStore) throws RemoteException;

}
