/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace serialize/parse in
 *                                                     unsafeGetObservation() with
 *                                                     ObservationUtil.shallowClone.
 *                                                     Reuse already created Key in
 *                                                     setContext().
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import static org.eclipse.leshan.server.californium.impl.CoapRequestBuilder.*;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.californium.core.observe.ObservationUtil;
import org.eclipse.californium.elements.CorrelationContext;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.RemoteCaliforniumRegistrationStore;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.eclipse.leshan.util.Key;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in memory store for registration and observation.
 */
public class RemoteInMemoryRegistrationStore implements RemoteCaliforniumRegistrationStore, Startable, Stoppable {
    private final Logger LOG = LoggerFactory.getLogger(RemoteInMemoryRegistrationStore.class);

    // Data structure
    private final Map<String /* end-point */, Registration> regsByEp = new HashMap<>();
    private Map<Key, org.eclipse.californium.core.observe.Observation> obsByToken = new HashMap<>();
    private Map<String, List<Key>> tokensByRegId = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private final long cleanPeriod; // in seconds

    public RemoteInMemoryRegistrationStore() {
        this(2); // default clean period : 2s
    }

    public RemoteInMemoryRegistrationStore(long cleanPeriodInSec) {
        this(Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(String.format("InMemoryRegistrationStore Cleaner (%ds)", cleanPeriodInSec))),
                cleanPeriodInSec);
    }

    public RemoteInMemoryRegistrationStore(ScheduledExecutorService schedExecutor, long cleanPeriodInSec) {
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
    }

    /* *************** Leshan Registration API **************** */

    @Override
    public Deregistration addRegistration(Registration registration) throws RemoteException {
        try {
            lock.writeLock().lock();

            Registration registrationRemoved = regsByEp.put(registration.getEndpoint(), registration);
            if (registrationRemoved != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registrationRemoved.getId());
                return new Deregistration(registrationRemoved, observationsRemoved);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    @Override
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) throws RemoteException {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(update.getRegistrationId());
            if (registration == null) {
                return null;
            } else {
                Registration updatedRegistration = update.update(registration);
                regsByEp.put(updatedRegistration.getEndpoint(), updatedRegistration);
                return new UpdatedRegistration(registration, updatedRegistration);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Registration getRegistrationById(String registrationId) {
        try {
            lock.readLock().lock();

            if (registrationId != null) {
                for (Registration registration : regsByEp.values()) {
                    if (registrationId.equals(registration.getId())) {
                        return registration;
                    }
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistration(String registrationId) throws RemoteException {
        return getRegistrationById(registrationId);
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) throws RemoteException {
        try {
            lock.readLock().lock();
            return regsByEp.get(endpoint);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByAdress(InetSocketAddress address) throws RemoteException {
        // TODO we should create an index instead of iterate all over the collection
        for (Registration r : regsByEp.values()) {
            if (address.getPort() == r.getPort() && address.getAddress().equals(r.getAddress())) {
                return r;
            }
        }
        return null;
    }

    @Override
    public Iterator<Registration> getAllRegistrations() {
        try {
            lock.readLock().lock();
            return new ArrayList<>(regsByEp.values()).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Deregistration removeRegistration(String registrationId) throws RemoteException {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistrationById(registrationId);
            if (registration != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registration.getId());
                regsByEp.remove(registration.getEndpoint());
                return new Deregistration(registration, observationsRemoved);
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Leshan Observation API **************** */

    /*
     * The observation is not persisted here, it is done by the Californium layer (in the implementation of the
     * org.eclipse.californium.core.observe.ObservationStore#add method)
     */
    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation) {

        List<Observation> removed = new ArrayList<>();

        try {
            lock.writeLock().lock();
            // cancel existing observations for the same path and registration id.
            for (Observation obs : unsafeGetObservations(registrationId)) {
                if (observation.getPath().equals(obs.getPath()) && !Arrays.equals(observation.getId(), obs.getId())) {
                    unsafeRemoveObservation(obs.getId());
                    removed.add(obs);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        return removed;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        try {
            lock.writeLock().lock();

            Observation observation = build(unsafeGetObservation(new Key(observationId)));
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                unsafeRemoveObservation(observationId);
                return observation;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Observation getObservation(String registrationId, byte[] observationId) {
        try {
            lock.readLock().lock();
            Observation observation = build(unsafeGetObservation(new Key(observationId)));
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                return observation;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try {
            lock.readLock().lock();
            return unsafeGetObservations(registrationId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try {
            lock.writeLock().lock();
            return unsafeRemoveAllObservations(registrationId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Californium ObservationStore API **************** */

    @Override
    public void add(org.eclipse.californium.core.observe.Observation obs) {
        if (obs != null) {
            try {
                lock.writeLock().lock();

                validateObservation(obs);

                String registrationId = extractRegistrationId(obs);
                Key token = new Key(obs.getRequest().getToken());
                org.eclipse.californium.core.observe.Observation previousObservation = obsByToken.put(token, obs);
                if (!tokensByRegId.containsKey(registrationId)) {
                    tokensByRegId.put(registrationId, new ArrayList<Key>());
                }
                tokensByRegId.get(registrationId).add(token);

                // log any collisions
                if (previousObservation != null) {
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(), obs.getRequest());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public org.eclipse.californium.core.observe.Observation get(byte[] token) {
        try {
            lock.readLock().lock();
            return unsafeGetObservation(new Key(token));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setContext(byte[] token, CorrelationContext ctx) {
        try {
            lock.writeLock().lock();
            Key key = new Key(token);
            org.eclipse.californium.core.observe.Observation obs = obsByToken.get(key);
            if (obs != null) {
                obsByToken.put(key, new org.eclipse.californium.core.observe.Observation(obs.getRequest(), ctx));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(byte[] token) {
        try {
            lock.writeLock().lock();
            unsafeRemoveObservation(token);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Observation utility functions **************** */

    private org.eclipse.californium.core.observe.Observation unsafeGetObservation(Key token) {
        org.eclipse.californium.core.observe.Observation obs = obsByToken.get(token);
        return ObservationUtil.shallowClone(obs);
    }

    private void unsafeRemoveObservation(byte[] observationId) {
        Key kToken = new Key(observationId);
        org.eclipse.californium.core.observe.Observation removed = obsByToken.remove(kToken);

        if (removed != null) {
            String registrationId = extractRegistrationId(removed);
            List<Key> tokens = tokensByRegId.get(registrationId);
            tokens.remove(kToken);
            if (tokens.isEmpty()) {
                tokensByRegId.remove(registrationId);
            }
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        List<Key> tokens = tokensByRegId.get(registrationId);
        if (tokens != null) {
            for (Key token : tokens) {
                Observation observationRemoved = build(obsByToken.remove(token));
                if (observationRemoved != null) {
                    removed.add(observationRemoved);
                }
            }
        }
        tokensByRegId.remove(registrationId);
        return removed;
    }

    private Collection<Observation> unsafeGetObservations(String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        List<Key> tokens = tokensByRegId.get(registrationId);
        if (tokens != null) {
            for (Key token : tokens) {
                Observation obs = build(unsafeGetObservation(token));
                if (obs != null) {
                    result.add(obs);
                }
            }
        }
        return result;
    }

    /* Retrieve the registrationId from the request context */
    private String extractRegistrationId(org.eclipse.californium.core.observe.Observation observation) {
        return observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID);
    }

    private Observation build(org.eclipse.californium.core.observe.Observation cfObs) {
        if (cfObs == null)
            return null;

        String regId = null;
        String lwm2mPath = null;
        Map<String, String> context = null;

        for (Entry<String, String> ctx : cfObs.getRequest().getUserContext().entrySet()) {
            switch (ctx.getKey()) {
            case CTX_REGID:
                regId = ctx.getValue();
                break;
            case CTX_LWM2M_PATH:
                lwm2mPath = ctx.getValue();
                break;
            default:
                if (context == null) {
                    context = new HashMap<>();
                }
                context.put(ctx.getKey(), ctx.getValue());
            }
        }
        return new Observation(cfObs.getRequest().getToken(), regId, new LwM2mPath(lwm2mPath), context);
    }

    private void validateObservation(org.eclipse.californium.core.observe.Observation observation) {
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_REGID))
            throw new IllegalStateException("missing registrationId info in the request context");
        if (!observation.getRequest().getUserContext().containsKey(CoapRequestBuilder.CTX_LWM2M_PATH))
            throw new IllegalStateException("missing lwm2m path info in the request context");
        if (getRegistrationById(observation.getRequest().getUserContext().get(CoapRequestBuilder.CTX_REGID)) == null) {
            throw new IllegalStateException("no registration for this Id");
        }
    }

    /* *************** Expiration handling **************** */

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        this.expirationListener = listener;
    }

    /**
     * start the registration store, will start regular cleanup of dead registrations.
     */
    @Override
    public void start() {
        schedExecutor.scheduleAtFixedRate(new Cleaner(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    @Override
    public void stop() {
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Clean up registration thread was interrupted.", e);
        }
    }

    private class Cleaner implements Runnable {

        @Override
        public void run() {
            try {
                Collection<Registration> allRegs = new ArrayList<>();
                try {
                    lock.readLock().lock();
                    allRegs.addAll(regsByEp.values());
                } finally {
                    lock.readLock().unlock();
                }

                for (Registration reg : allRegs) {
                    if (!reg.isAlive()) {
                        // force de-registration
                        Deregistration removedRegistration = removeRegistration(reg.getId());
                        expirationListener.registrationExpired(removedRegistration.getRegistration(),
                                removedRegistration.getObservations());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }
}
