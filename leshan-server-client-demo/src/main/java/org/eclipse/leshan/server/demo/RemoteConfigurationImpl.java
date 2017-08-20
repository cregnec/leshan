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

import java.rmi.RemoteException;

import org.eclipse.leshan.server.registration.RemoteRegistrationService;

public class RemoteConfigurationImpl implements RemoteConfiguration {
    private RemoteRegistrationService registrationService;
    private Object synchronObject;

    public RemoteConfigurationImpl() {

    }

    public Object getSynchronizationObject() {
        return this.synchronObject;
    }

    public void setSynchronizationObject(Object object) {
        this.synchronObject = object;
    }

    public RemoteRegistrationService getRegistrationService() {
        return this.registrationService;
    }

    @Override
    public void setRegistrationService(RemoteRegistrationService registrationService) throws RemoteException {
        this.registrationService = registrationService;

        synchronized (this.synchronObject) {
            this.synchronObject.notify();
        }
    }

}
