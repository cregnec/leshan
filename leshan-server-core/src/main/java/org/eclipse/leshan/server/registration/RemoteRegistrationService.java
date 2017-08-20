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
package org.eclipse.leshan.server.registration;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import org.eclipse.leshan.core.observation.Observation;

/**
 * A service to access registered clients
 */
public interface RemoteRegistrationService extends Remote {

    /**
     * Retrieves a registration by id.
     * 
     * @param id registration id
     * @return the matching registration or <code>null</code> if not found
     */
    Registration getById(String id) throws RemoteException;

    /**
     * Fire registered event.
     * 
     * @param registration current registration
     * @param previousReg previous registration
     * @param previousObservations previous observations.
     */
    void fireRegistered(Registration registration, Registration previousReg,
            Collection<Observation> previousObsersations) throws RemoteException;

    /**
     * Fire unregistered event.
     * 
     * @param registration current registration
     * @param observations observations
     * @param newReg new registration.
     */
    void fireUnregistered(Registration registration, Collection<Observation> observations, Registration newReg)
            throws RemoteException;

    /**
     * Fire updated event.
     * 
     * @param update registration update
     * @param registration updated registration
     * @param previousRegistration previous registration.
     * @return the matching registration or <code>null</code> if not found
     */
    void fireUpdated(RegistrationUpdate update, Registration updatedRegistration, Registration previousRegistration)
            throws RemoteException;

    /**
     * Retrieves the registration store.
     * 
     * @return the registration store or <code>null</code> if not set
     */
    RegistrationStore getStore() throws RemoteException;
}
