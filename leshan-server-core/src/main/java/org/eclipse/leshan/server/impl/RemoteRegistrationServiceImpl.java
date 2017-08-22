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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.RemoteRegistrationListener;
import org.eclipse.leshan.server.registration.RemoteRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link RemoteRegistrationService}
 */
public class RemoteRegistrationServiceImpl implements RemoteRegistrationService, ExpirationListener {

    private final List<RemoteRegistrationListener> listeners = new CopyOnWriteArrayList<>();

    private RegistrationStore store;

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRegistrationServiceImpl.class);

    public RemoteRegistrationServiceImpl(RegistrationStore store) {
        this.store = store;
        store.setExpirationListener(this);
    }

    @Override
    public void addListener(RemoteRegistrationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(RemoteRegistrationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public List<Registration> getAllRegistrations() throws RemoteException {
        List<Registration> registrations = new ArrayList<Registration>();
        for (Iterator<Registration> iterator = store.getAllRegistrations(); iterator.hasNext();) {
            registrations.add(iterator.next());
        }
        return registrations;
    }

    @Override
    public Registration getByEndpoint(String endpoint) throws RemoteException {
        return store.getRegistrationByEndpoint(endpoint);
    }

    public Registration getById(String id) {
        return store.getRegistration(id);
    }

    @Override
    public void registrationExpired(Registration registration, Collection<Observation> observations) {
        try {
            for (RemoteRegistrationListener l : listeners) {
                l.unregistered(registration, observations, true, null);
            }
        } catch (RemoteException e) {
            LOG.error("Failed to fire expired via RMI", e);
        }
    }

    public void fireRegistered(Registration registration, Registration previousReg,
            Collection<Observation> previousObsersations) {
        try {
            for (RemoteRegistrationListener l : listeners) {
                l.registered(registration, previousReg, previousObsersations);
            }
        } catch (RemoteException e) {
            LOG.error("Failed to fire registered via RMI", e);
        }
    }

    public void fireUnregistered(Registration registration, Collection<Observation> observations, Registration newReg) {
        try {
            for (RemoteRegistrationListener l : listeners) {
                l.unregistered(registration, observations, false, newReg);
            }
        } catch (RemoteException e) {
            LOG.error("Failed to fire unregistered via RMI", e);
        }
    }

    public void fireUpdated(RegistrationUpdate update, Registration updatedRegistration,
            Registration previousRegistration) {
        try {
            for (RemoteRegistrationListener l : listeners) {
                l.updated(update, updatedRegistration, previousRegistration);
            }
        } catch (RemoteException e) {
            LOG.error("Failed to fire updated via RMI", e);
        }
    }

    public RegistrationStore getStore() {
        return store;
    }
}
