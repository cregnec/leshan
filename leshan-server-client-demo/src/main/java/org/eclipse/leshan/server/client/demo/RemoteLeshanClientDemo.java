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
 *     Bosch Software Innovations - added Redis URL support with authentication
 *     Firis SA - added mDNS services registering 
 *******************************************************************************/
package org.eclipse.leshan.server.client.demo;

import java.net.BindException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyStore;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.server.californium.RemoteCaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.impl.RemoteInMemoryRegistrationStore;
import org.eclipse.leshan.server.californium.impl.RemoteLeshanServer;
import org.eclipse.leshan.server.demo.RemoteConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteLeshanClientDemo {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteLeshanClientDemo.class);

    private final static String[] modelPaths = new String[] { "31024.xml",

                            "10241.xml", "10242.xml", "10243.xml", "10244.xml", "10245.xml", "10246.xml", "10247.xml",
                            "10248.xml", "10249.xml", "10250.xml",

                            "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml",
                            "2055.xml", "2056.xml", "2057.xml",

                            "3200.xml", "3201.xml", "3202.xml", "3203.xml", "3300.xml", "3301.xml", "3302.xml",
                            "3303.xml", "3304.xml", "3305.xml", "3306.xml", "3308.xml", "3310.xml", "3311.xml",
                            "3312.xml", "3313.xml", "3314.xml", "3315.xml", "3316.xml", "3317.xml", "3318.xml",
                            "3319.xml", "3320.xml", "3321.xml", "3322.xml", "3323.xml", "3324.xml", "3325.xml",
                            "3326.xml", "3327.xml", "3328.xml", "3329.xml", "3330.xml", "3331.xml", "3332.xml",
                            "3333.xml", "3334.xml", "3335.xml", "3336.xml", "3337.xml", "3338.xml", "3339.xml",
                            "3340.xml", "3341.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346.xml",
                            "3347.xml", "3348.xml",

                            "Communication_Characteristics-V1_0.xml",

                            "LWM2M_Lock_and_Wipe-V1_0.xml", "LWM2M_Cellular_connectivity-v1_0.xml",
                            "LWM2M_APN_connection_profile-v1_0.xml", "LWM2M_WLAN_connectivity4-v1_0.xml",
                            "LWM2M_Bearer_selection-v1_0.xml", "LWM2M_Portfolio-v1_0.xml", "LWM2M_DevCapMgmt-v1_0.xml",
                            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",

                            "Non-Access_Stratum_NAS_configuration-V1_0.xml" };

    private final static String USAGE = "java -jar leshan-server-demo.jar [OPTION]";

    private final static String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();

    private final static String DEFAULT_KEYSTORE_ALIAS = "leshan";

    private static RemoteCaliforniumRegistrationStore registrationStore;

    public static void main(String[] args) {
        // Define options for command line tools
        Options options = new Options();

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("lh", "coaphost", true, "Set the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true,
                String.format("Set the local CoAP port.\n  Default: %d.", LwM2m.DEFAULT_COAP_PORT));
        options.addOption("slh", "coapshost", true, "Set the secure local CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true,
                String.format("Set the secure local CoAP port.\nDefault: %d.", LwM2m.DEFAULT_COAP_SECURE_PORT));
        options.addOption("ks", "keystore", true,
                "Set the key store file. If set, X.509 mode is enabled, otherwise built-in RPK credentials are used.");
        options.addOption("ksp", "storepass", true, "Set the key store password.");
        options.addOption("kst", "storetype", true,
                String.format("Set the key store type.\nDefault: %s.", DEFAULT_KEYSTORE_TYPE));
        options.addOption("ksa", "alias", true, String.format(
                "Set the key store alias to use for server credentials.\nDefault: %s.", DEFAULT_KEYSTORE_ALIAS));
        options.addOption("ksap", "keypass", true, "Set the key store alias password to use.");
        options.addOption("wp", "webport", true, "Set the HTTP port for web server.\nDefault: 8080.");
        options.addOption("m", "modelsfolder", true, "A folder which contains object models in OMA DDF(.xml) format.");
        options.addOption("r", "redis", true,
                "Set the location of the Redis database for running in cluster mode. The URL is in the format of: 'redis://:password@hostname:port/db_number'\nExample without DB and password: 'redis://localhost:6379'\nDefault: none, no Redis connection.");
        options.addOption("mdns", "publishDNSSdServices", false, "Publish leshan's services to DNS Service discovery");
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);

        // Parse arguments
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Print help
        if (cl.hasOption("help")) {
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if unexpected options
        if (cl.getArgs().length > 0) {
            System.err.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, options);
            return;
        }

        // get local address
        String localAddress = cl.getOptionValue("lh");
        String localPortOption = cl.getOptionValue("lp");
        int localPort = LwM2m.DEFAULT_COAP_PORT;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // get secure local address
        String secureLocalAddress = cl.getOptionValue("slh");
        String secureLocalPortOption = cl.getOptionValue("slp");
        int secureLocalPort = LwM2m.DEFAULT_COAP_SECURE_PORT;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // get http port
        String webPortOption = cl.getOptionValue("wp");
        int webPort = 8080;
        if (webPortOption != null) {
            webPort = Integer.parseInt(webPortOption);
        }

        // Get models folder
        String modelsFolderPath = cl.getOptionValue("m");

        // get the Redis hostname:port
        String redisUrl = cl.getOptionValue("r");

        // Get keystore parameters
        String keyStorePath = cl.getOptionValue("ks");
        String keyStoreType = cl.getOptionValue("kst", KeyStore.getDefaultType());
        String keyStorePass = cl.getOptionValue("ksp");
        String keyStoreAlias = cl.getOptionValue("ksa");
        String keyStoreAliasPass = cl.getOptionValue("ksap");

        // Get mDNS publish switch
        Boolean publishDNSSdServices = cl.hasOption("mdns");

        try {
            createAndStartServer(webPort);
        } catch (BindException e) {
            System.err.println(
                    String.format("Web port %s is already used, you could change it using 'webport' option.", webPort));
            formatter.printHelp(USAGE, options);
        } catch (Exception e) {
            LOG.error("Remote Leshan server stopped with unexpected error ...", e);
        }
    }

    public static void createAndStartServer(int webPort) throws Exception {

        registrationStore = new RemoteInMemoryRegistrationStore();

        try {
            LOG.info("Getting RMI registry");
            Registry registry = LocateRegistry.getRegistry();

            RemoteConfiguration rmtConfig = (RemoteConfiguration) registry.lookup(RemoteConfiguration.LOOKUPNAME);

            LOG.info("Getting RemoteLeshanServer");
            RemoteLeshanServer lwServer = (RemoteLeshanServer) registry.lookup(RemoteLeshanServer.LOOKUPNAME);

            LOG.info("Setting registration service");
            RemoteCaliforniumRegistrationStore stub = (RemoteCaliforniumRegistrationStore) UnicastRemoteObject
                    .exportObject(registrationStore, 0);
            rmtConfig.setRegistrationStore(stub);

        } catch (RemoteException e) {
            LOG.error("Failed to get RMI registry", e);
        } catch (NotBoundException e) {
            LOG.error("Failed to lookup RMI object", e);
        }
        // Now prepare Jetty

        // Server server = new Server(webPort);
        // WebAppContext root = new WebAppContext(); root.setContextPath("/");
        // root.setResourceBase(LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        // root.setParentLoaderPriority(true); server.setHandler(root);
        //
        // // Create Servlet
        // EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecureAddress().getPort());
        // ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        // root.addServlet(eventServletHolder, "/event/*");
        //
        // ServletHolder clientServletHolder = new ServletHolder( new ClientServlet(lwServer,
        // lwServer.getSecureAddress().getPort())); root.addServlet(clientServletHolder, "/api/clients/*");
        //
        // ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, publicKey));
        // root.addServlet(securityServletHolder, "/api/security/*");
        //
        // ServletHolder objectSpecServletHolder = new ServletHolder(new
        // ObjectSpecServlet(lwServer.getModelProvider())); root.addServlet(objectSpecServletHolder,
        // "/api/objectspecs/*");
        //
        // // Register a service to DNS-SD if (publishDNSSdServices) {
        //
        // // Create a JmDNS instance JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
        //
        // // Publish Leshan HTTP Service ServiceInfo httpServiceInfo = ServiceInfo.create("_http._tcp.local.",
        // "leshan", webPort, ""); jmdns.registerService(httpServiceInfo);
        //
        // // Publish Leshan CoAP Service ServiceInfo coapServiceInfo = ServiceInfo.create("_coap._udp.local.",
        // "leshan", localPort, ""); jmdns.registerService(coapServiceInfo);
        //
        // // Publish Leshan Secure CoAP Service ServiceInfo coapSecureServiceInfo =
        // ServiceInfo.create("_coaps._udp.local.", "leshan", secureLocalPort, "");
        // jmdns.registerService(coapSecureServiceInfo); }

        // Start Jetty & Leshan
        /*
         * server.start(); LOG.info("Web server started at {}.", server.getURI());
         */
    }
}
