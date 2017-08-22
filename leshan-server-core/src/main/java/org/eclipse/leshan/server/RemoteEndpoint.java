/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
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
 *    Matthias Kovatsch - creator and main architect
 *    Martin Lanter - architect and re-implementation
 *    Dominique Im Obersteg - parsers and initial implementation
 *    Daniel Pauli - parsers and initial implementation
 *    Kai Hudalla - logging
 ******************************************************************************/
package org.eclipse.leshan.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A communication endpoint multiplexing CoAP message exchanges between (potentially multiple) clients and servers.
 * 
 * An Endpoint is bound to a particular IP address and port. Clients use an Endpoint to send a request to a server.
 * Servers bind resources to one or more Endpoints in order for them to be requested over the network by clients.
 */
public interface RemoteEndpoint extends Remote {
    // /**
    // * Adds the observer to the list of observers. This has nothing to do with CoAP observe relations.
    // *
    // * @param obs the observer
    // */
    // void addObserver(EndpointObserver obs);
    //
    // /**
    // * Removes the endpoint observer.This has nothing to do with CoAP observe relations.
    // *
    // * @param obs the observer
    // */
    // void removeObserver(EndpointObserver obs);
    //
    // /**
    // * Adds a listener for observe notification (This is related to CoAP observe)
    // *
    // * @param lis the listener
    // */
    // void addNotificationListener(NotificationListener lis);
    //
    // /**
    // * Removes a listener for observe notification (This is related to CoAP observe)
    // *
    // * @param lis the listener
    // */
    // void removeNotificationListener(NotificationListener lis);

    /**
     * Adds a message interceptor to this endpoint.
     *
     * @param interceptor the interceptor
     */
    void addInterceptor(RemoteMessageInterceptor interceptor) throws RemoteException;

    /**
     * Removes the interceptor.
     *
     * @param interceptor the interceptor
     */
    void removeInterceptor(RemoteMessageInterceptor interceptor) throws RemoteException;

}
