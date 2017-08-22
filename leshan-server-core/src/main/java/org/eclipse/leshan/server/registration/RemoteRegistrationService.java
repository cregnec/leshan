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
import java.util.List;

/**
 * A service to access registered clients
 */
public interface RemoteRegistrationService extends Remote {

    /**
     * Retrieves a registration by end-point.
     * 
     * @param endpoint
     * @return the matching registration or <code>null</code> if not found
     */
    Registration getByEndpoint(String endpoint) throws RemoteException;

    /**
     * Returns an List of all registrations. This is necessary in order to pass them over RMI.
     *
     * @return an <tt>List</tt> over registrations
     */
    List<Registration> getAllRegistrations() throws RemoteException;

    /**
     * Adds a new listener to be notified with client registration events.
     * 
     * @param listener
     */
    void addListener(RemoteRegistrationListener listener) throws RemoteException;

    /**
     * Removes a client registration listener.
     * 
     * @param listener the listener to be removed
     */
    void removeListener(RemoteRegistrationListener listener) throws RemoteException;

}
