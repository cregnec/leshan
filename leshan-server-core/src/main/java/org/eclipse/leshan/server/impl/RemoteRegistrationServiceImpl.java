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
package org.eclipse.leshan.server.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.RemoteRegistrationService;

/**
 * An implementation of {@link RemoteRegistrationService}
 */
public class RemoteRegistrationServiceImpl implements RemoteRegistrationService, ExpirationListener {

    private final List<RegistrationListener> listeners = new CopyOnWriteArrayList<>();

    private RegistrationStore store;

    public RemoteRegistrationServiceImpl(RegistrationStore store) {
        this.store = store;
        store.setExpirationListener(this);
    }

    public void addListener(RegistrationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RegistrationListener listener) {
        listeners.remove(listener);
    }

    public Iterator<Registration> getAllRegistrations() {
        return store.getAllRegistrations();
    }

    public Registration getByEndpoint(String endpoint) {
        return store.getRegistrationByEndpoint(endpoint);
    }

    @Override
    public Registration getById(String id) {
        return store.getRegistration(id);
    }

    @Override
    public void registrationExpired(Registration registration, Collection<Observation> observations) {
        for (RegistrationListener l : listeners) {
            l.unregistered(registration, observations, true, null);
        }
    }

    @Override
    public void fireRegistered(Registration registration, Registration previousReg,
            Collection<Observation> previousObsersations) {
        for (RegistrationListener l : listeners) {
            l.registered(registration, previousReg, previousObsersations);
        }
    }

    @Override
    public void fireUnregistered(Registration registration, Collection<Observation> observations, Registration newReg) {
        for (RegistrationListener l : listeners) {
            l.unregistered(registration, observations, false, newReg);
        }
    }

    @Override
    public void fireUpdated(RegistrationUpdate update, Registration updatedRegistration,
            Registration previousRegistration) {
        for (RegistrationListener l : listeners) {
            l.updated(update, updatedRegistration, previousRegistration);
        }
    }

    @Override
    public RegistrationStore getStore() {
        return store;
    }
}
