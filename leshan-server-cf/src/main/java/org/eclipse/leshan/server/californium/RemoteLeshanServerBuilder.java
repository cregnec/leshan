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
package org.eclipse.leshan.server.californium;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.impl.InMemoryRegistrationStore;
import org.eclipse.leshan.server.californium.impl.LwM2mPskStore;
import org.eclipse.leshan.server.californium.impl.RemoteLeshanServer;
import org.eclipse.leshan.server.californium.impl.RemoteObservationServiceImpl;
import org.eclipse.leshan.server.impl.InMemorySecurityStore;
import org.eclipse.leshan.server.impl.RemoteRegistrationServiceImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class helping you to build and configure a Californium based Leshan Lightweight M2M server. Usage: create it, call
 * the different setters for changing the configuration and then call the {@link #build()} method for creating the
 * {@link LwM2mServer} ready to operate.
 */
public class RemoteLeshanServerBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteLeshanServerBuilder.class);

    private CaliforniumRegistrationStore registrationStore;
    private RemoteRegistrationServiceImpl registrationService;
    private RemoteObservationServiceImpl observationService;
    private SecurityStore securityStore;
    private LwM2mModelProvider modelProvider;
    private Authorizer authorizer;

    private InetSocketAddress localAddress;
    private InetSocketAddress localSecureAddress;

    private LwM2mNodeEncoder encoder;
    private LwM2mNodeDecoder decoder;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private X509Certificate[] certificateChain;
    private Certificate[] trustedCertificates;

    private NetworkConfig coapConfig;
    private DtlsConnectorConfig.Builder dtlsConfigBuilder;

    /**
     * <p>
     * Set the address/port for unsecured CoAP Server.
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5683) is used
     * 
     * @param hostname The address to bind. If null wildcard address is used.
     * @param port A valid port value is between 0 and 65535. A port number of zero will let the system pick up an
     *        ephemeral port in a bind operation.
     */
    public RemoteLeshanServerBuilder setLocalAddress(String hostname, int port) {
        if (hostname == null) {
            this.localAddress = new InetSocketAddress(port);
        } else {
            this.localAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    /**
     * <p>
     * Set the address for unsecured CoAP Server.
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5683) is used.
     */
    public RemoteLeshanServerBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    /**
     * <p>
     * Set the address/port for secured CoAP Server (Using DTLS).
     * <p>
     * 
     * By default a wildcard address and the default CoAPs port(5684) is used.
     * 
     * @param hostname The address to bind. If null wildcard address is used.
     * @param port A valid port value is between 0 and 65535. A port number of zero will let the system pick up an
     *        ephemeral port in a bind operation.
     */
    public RemoteLeshanServerBuilder setLocalSecureAddress(String hostname, int port) {
        if (hostname == null) {
            this.localSecureAddress = new InetSocketAddress(port);
        } else {
            this.localSecureAddress = new InetSocketAddress(hostname, port);
        }
        return this;
    }

    /**
     * <p>
     * Set the address for secured CoAP Server (Using DTLS).
     * </p>
     * 
     * By default a wildcard address and the default CoAP port(5684) is used.
     */
    public RemoteLeshanServerBuilder setLocalSecureAddress(InetSocketAddress localSecureAddress) {
        this.localSecureAddress = localSecureAddress;
        return this;
    }

    /**
     * <p>
     * Set your {@link RegistrationStore} implementation which stores {@link Registration} and {@link Observation}.
     * </p>
     * By default the {@link InMemoryRegistrationStore} implementation is used.
     * 
     */
    public RemoteLeshanServerBuilder setRegistrationStore(CaliforniumRegistrationStore registrationStore) {
        this.registrationStore = registrationStore;
        return this;
    }

    /**
     * <p>
     * Set your {@link RegistrationService}
     * </p>
     * 
     */
    public RemoteLeshanServerBuilder setRegistrationService(RemoteRegistrationServiceImpl registrationService) {
        this.registrationService = registrationService;
        return this;
    }

    /**
     * <p>
     * Set your {@link ObservationService}
     * </p>
     * 
     */
    public RemoteLeshanServerBuilder setObservationService(RemoteObservationServiceImpl observationService) {
        this.observationService = observationService;
        return this;
    }

    /**
     * <p>
     * Set your {@link SecurityStore} implementation which stores {@link SecurityInfo}.
     * </p>
     * By default no security store is set and the secure CoAP Server is not launched. An {@link InMemorySecurityStore}
     * is provided to start using secure connection.
     * 
     */
    public RemoteLeshanServerBuilder setSecurityStore(SecurityStore securityStore) {
        this.securityStore = securityStore;
        return this;
    }

    /**
     * <p>
     * Set your {@link Authorizer} implementation to define if a device if authorize to register to this server.
     * </p>
     * By default the {@link DefaultAuthorizer} implementation is used, if a security store is set.
     * 
     */
    public RemoteLeshanServerBuilder setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
        return this;
    }

    /**
     * <p>
     * Set your {@link LwM2mModelProvider} implementation.
     * </p>
     * By default the {@link StandardModelProvider} implementation is used which support all core objects for all
     * devices.
     * 
     */
    public RemoteLeshanServerBuilder setObjectModelProvider(LwM2mModelProvider objectModelProvider) {
        this.modelProvider = objectModelProvider;
        return this;
    }

    /**
     * <p>
     * Set the {@link PublicKey} of the server which will be used for RawPublicKey DTLS authentication.
     * </p>
     * This should be used for RPK support only. If you support RPK and X509,
     * {@link RemoteLeshanServerBuilder#setCertificateChain(X509Certificate[])} should be used.
     */
    public RemoteLeshanServerBuilder setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Set the {@link PrivateKey} of the server which will be used for RawPublicKey(RPK) and X509 DTLS authentication.
     */
    public RemoteLeshanServerBuilder setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    /**
     * <p>
     * Set the CertificateChain of the server which will be used for X509 DTLS authentication.
     * </p>
     * For RPK the public key will be extract from the first X509 certificate of the certificate chain. If you only need
     * RPK support, use {@link RemoteLeshanServerBuilder#setPublicKey(PublicKey)} instead.
     */
    public RemoteLeshanServerBuilder setCertificateChain(X509Certificate[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    /**
     * The list of trusted certificates used to authenticate devices.
     */
    public RemoteLeshanServerBuilder setTrustedCertificates(Certificate[] trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mNodeEncoder} which will encode {@link LwM2mNode} with supported content format.
     * </p>
     * By default the {@link DefaultLwM2mNodeEncoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public RemoteLeshanServerBuilder setEncoder(LwM2mNodeEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * <p>
     * Set the {@link LwM2mNodeDecoder} which will decode data in supported content format to create {@link LwM2mNode}.
     * </p>
     * By default the {@link DefaultLwM2mNodeDecoder} is used. It supports Text, Opaque, TLV and JSON format.
     */
    public RemoteLeshanServerBuilder setDecoder(LwM2mNodeDecoder decoder) {
        this.decoder = decoder;
        return this;
    }

    /**
     * Set the Californium/CoAP {@link NetworkConfig}.
     */
    public RemoteLeshanServerBuilder setCoapConfig(NetworkConfig config) {
        this.coapConfig = config;
        return this;
    }

    /**
     * Set the Scandium/DTLS Configuration : {@link DtlsConnectorConfig.Builder}.
     */
    public RemoteLeshanServerBuilder setDtlsConfig(DtlsConnectorConfig.Builder config) {
        this.dtlsConfigBuilder = config;
        return this;
    }

    public RemoteLeshanServer build() {
        if (localAddress == null)
            localAddress = new InetSocketAddress(LwM2m.DEFAULT_COAP_PORT);
        // TODO keep this registrationStore while it is being used by observationService
        if (registrationStore == null)
            registrationStore = new InMemoryRegistrationStore();
        /*
         * if (registrationService == null) registrationService = new RemoteRegistrationServiceImpl(registrationStore);
         */
        if (authorizer == null)
            authorizer = new DefaultAuthorizer(securityStore);
        if (modelProvider == null)
            modelProvider = new StandardModelProvider();
        if (encoder == null)
            encoder = new DefaultLwM2mNodeEncoder();
        if (decoder == null)
            decoder = new DefaultLwM2mNodeDecoder();
        if (observationService == null)
            observationService = new RemoteObservationServiceImpl(registrationStore, modelProvider, decoder);
        if (coapConfig == null) {
            coapConfig = new NetworkConfig();
            coapConfig.set(NetworkConfig.Keys.MID_TRACKER, "NULL");
        }

        // handle dtlsConfig
        DtlsConnectorConfig dtlsConfig = null;
        if (securityStore != null && dtlsConfigBuilder == null) {
            dtlsConfigBuilder = new DtlsConnectorConfig.Builder();
            // TODO remove 2 lines below when we will integrate the californium v2.0.0-M5/RC1
            dtlsConfigBuilder.setMaxConnections(coapConfig.getInt(Keys.MAX_ACTIVE_PEERS));
            dtlsConfigBuilder.setStaleConnectionThreshold(coapConfig.getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));
        }

        if (dtlsConfigBuilder != null) {
            DtlsConnectorConfig incompleteConfig = dtlsConfigBuilder.getIncompleteConfig();
            // Handle PSK Store
            if (incompleteConfig.getPskStore() == null) {
                dtlsConfigBuilder.setPskStore(new LwM2mPskStore(this.securityStore, registrationStore));
            } else {
                LOG.warn(
                        "PskStore should be automatically set by Leshan. Using a custom implementation is not advised.");
            }

            // Handle secure address
            if (incompleteConfig.getAddress() == null) {
                if (localSecureAddress == null) {
                    localSecureAddress = new InetSocketAddress(LwM2m.DEFAULT_COAP_SECURE_PORT);
                }
                dtlsConfigBuilder.setAddress(localSecureAddress);
            } else if (localSecureAddress != null && !localSecureAddress.equals(incompleteConfig.getAddress())) {
                throw new IllegalStateException(String.format(
                        "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for secure address: %s != %s",
                        localSecureAddress, incompleteConfig.getAddress()));
            }

            // Handle active peers
            if (incompleteConfig.getMaxConnections() == null)
                dtlsConfigBuilder.setMaxConnections(coapConfig.getInt(Keys.MAX_ACTIVE_PEERS));
            if (incompleteConfig.getStaleConnectionThreshold() == null)
                dtlsConfigBuilder.setStaleConnectionThreshold(coapConfig.getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));

            // handle trusted certificates
            if (trustedCertificates != null) {
                if (incompleteConfig.getTrustStore() == null) {
                    dtlsConfigBuilder.setTrustStore(trustedCertificates);
                } else if (!Arrays.equals(trustedCertificates, incompleteConfig.getTrustStore())) {
                    throw new IllegalStateException(String.format(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for trusted Certificates (trustStore) : \n%s != \n%s",
                            Arrays.toString(trustedCertificates), Arrays.toString(incompleteConfig.getTrustStore())));
                }
            }

            // check conflict for private key
            if (privateKey != null) {
                if (incompleteConfig.getPrivateKey() != null && !incompleteConfig.getPrivateKey().equals(privateKey)) {
                    throw new IllegalStateException(String.format(
                            "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for private key: %s != %s",
                            privateKey, incompleteConfig.getPrivateKey()));
                }

                // if in raw key mode and not in X.509 set the raw keys
                if (certificateChain == null && publicKey != null) {
                    if (incompleteConfig.getPublicKey() != null && !incompleteConfig.getPublicKey().equals(publicKey)) {
                        throw new IllegalStateException(String.format(
                                "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for public key: %s != %s",
                                publicKey, incompleteConfig.getPublicKey()));
                    }

                    dtlsConfigBuilder.setIdentity(privateKey, publicKey);
                }
                // if in X.509 mode set the private key, certificate chain, public key is extracted from the certificate
                if (certificateChain != null && certificateChain.length > 0) {
                    if (incompleteConfig.getCertificateChain() != null
                            && !Arrays.equals(incompleteConfig.getCertificateChain(), certificateChain)) {
                        throw new IllegalStateException(String.format(
                                "Configuration conflict between LeshanBuilder and DtlsConnectorConfig.Builder for certificate chain: %s != %s",
                                certificateChain, incompleteConfig.getCertificateChain()));
                    }

                    dtlsConfigBuilder.setIdentity(privateKey, certificateChain, false);
                }

            }
            dtlsConfig = dtlsConfigBuilder.build();
        }

        return new RemoteLeshanServer(localAddress, registrationStore, registrationService, observationService,
                securityStore, authorizer, modelProvider, encoder, decoder, coapConfig, dtlsConfig);
    }
}
