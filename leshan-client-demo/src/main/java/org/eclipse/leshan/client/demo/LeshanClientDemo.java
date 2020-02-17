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
 *     Zebra Technologies - initial API and implementation
 *     Sierra Wireless, - initial API and implementation
 *     Bosch Software Innovations GmbH, - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.client.demo;

import static org.eclipse.leshan.LwM2mId.*;
import static org.eclipse.leshan.client.object.Security.*;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
<<<<<<< HEAD
import org.eclipse.leshan.client.demo.mt.GroupSensors;
=======
import org.eclipse.leshan.client.resource.listener.ObjectsListenerAdapter;
import org.eclipse.leshan.core.model.LwM2mModel;
>>>>>>> de4e30bde301f134bcfc499504ca2c9d4a98efae
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Hex;
import org.eclipse.leshan.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeshanClientDemo {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClientDemo.class);

<<<<<<< HEAD
    private final static String[] modelPaths = new String[] { "3303.xml",
        "33755.xml" , "33756.xml", "33757.xml", "33758.xml"};
=======
    // /!\ This class is a COPY of org.eclipse.leshan.server.demo.LeshanServerDemo.modelPaths /!\
    // TODO create a leshan-demo project ?
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
                            "3347.xml", "3348.xml", "3349.xml", "3350.xml",

                            "Communication_Characteristics-V1_0.xml",

                            "LWM2M_Lock_and_Wipe-V1_0.xml", "LWM2M_Cellular_connectivity-v1_0.xml",
                            "LWM2M_APN_connection_profile-v1_0.xml", "LWM2M_WLAN_connectivity4-v1_0.xml",
                            "LWM2M_Bearer_selection-v1_0.xml", "LWM2M_Portfolio-v1_0.xml", "LWM2M_DevCapMgmt-v1_0.xml",
                            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",

                            "Non-Access_Stratum_NAS_configuration-V1_0.xml" };
>>>>>>> de4e30bde301f134bcfc499504ca2c9d4a98efae

    //private static final int OBJECT_ID_TEMPERATURE_SENSOR = 3303;
    private static final int OBJECT_ID_GROUP_DATA = 33755;
    private static final int OBJECT_ID_DEVICES = 33756;
    private static final int OBJECT_ID_SENSORS = 33758;
    private static final int OBJECT_ID_SMOKE  = 33757;
    private final static String DEFAULT_ENDPOINT = "LeshanClientDemo";
    private final static int DEFAULT_LIFETIME = 5 * 60; // 5min in seconds
    private final static String USAGE = "java -jar leshan-client-demo.jar [OPTION]\n\n";

    private static MyLocation locationInstance;

    public static void main(final String[] args) {
        // Define options for command line tools
        Options options = new Options();

        final StringBuilder PSKChapter = new StringBuilder();
        PSKChapter.append("\n .");
        PSKChapter.append("\n .");
        PSKChapter.append("\n ================================[ PSK ]=================================");
        PSKChapter.append("\n | By default Leshan demo use non secure connection.                    |");
        PSKChapter.append("\n | To use PSK, -i and -p options should be used together.               |");
        PSKChapter.append("\n ------------------------------------------------------------------------");

        final StringBuilder RPKChapter = new StringBuilder();
        RPKChapter.append("\n .");
        RPKChapter.append("\n .");
        RPKChapter.append("\n ================================[ RPK ]=================================");
        RPKChapter.append("\n | By default Leshan demo use non secure connection.                    |");
        RPKChapter.append("\n | To use RPK, -cpubk -cprik -spubk options should be used together.    |");
        RPKChapter.append("\n | To get helps about files format and how to generate it, see :        |");
        RPKChapter.append("\n | See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        RPKChapter.append("\n ------------------------------------------------------------------------");

        final StringBuilder X509Chapter = new StringBuilder();
        X509Chapter.append("\n .");
        X509Chapter.append("\n .");
        X509Chapter.append("\n ================================[X509]==================================");
        X509Chapter.append("\n | By default Leshan demo use non secure connection.                    |");
        X509Chapter.append("\n | To use X509, -ccert -cprik -scert options should be used together.   |");
        X509Chapter.append("\n | To get helps about files format and how to generate it, see :        |");
        X509Chapter.append("\n | See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        X509Chapter.append("\n ------------------------------------------------------------------------");

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("n", true, String.format(
                "Set the endpoint name of the Client.\nDefault: the local hostname or '%s' if any.", DEFAULT_ENDPOINT));
        options.addOption("b", false, "If present use bootstrap.");
        options.addOption("l", true, String.format(
                "The lifetime in seconds used to register, ignored if -b is used.\n Default : %ds", DEFAULT_LIFETIME));
        options.addOption("lh", true, "Set the local CoAP address of the Client.\n  Default: any local address.");
        options.addOption("lp", true,
                "Set the local CoAP port of the Client.\n  Default: A valid port value is between 0 and 65535.");
        options.addOption("u", true, String.format("Set the LWM2M or Bootstrap server URL.\nDefault: localhost:%d.",
                LwM2m.DEFAULT_COAP_PORT));
        options.addOption("pos", true,
                "Set the initial location (latitude, longitude) of the device to be reported by the Location object.\n Format: lat_float:long_float");
        options.addOption("sf", true, "Scale factor to apply when shifting position.\n Default is 1.0." + PSKChapter);
        options.addOption("i", true, "Set the LWM2M or Bootstrap server PSK identity in ascii.");
        options.addOption("p", true, "Set the LWM2M or Bootstrap server Pre-Shared-Key in hexa." + RPKChapter);
        options.addOption("cpubk", true,
                "The path to your client public key file.\n The public Key should be in SubjectPublicKeyInfo format (DER encoding).");
        options.addOption("cprik", true,
                "The path to your client private key file.\nThe private key should be in PKCS#8 format (DER encoding).");
        options.addOption("spubk", true,
                "The path to your server public key file.\n The public Key should be in SubjectPublicKeyInfo format (DER encoding)."
                        + X509Chapter);
        options.addOption("ccert", true,
                "The path to your client certificate file.\n The certificate Common Name (CN) should generaly be equal to the client endpoint name (see -n option).\nThe certificate should be in X509v3 format (DER encoding).");
        options.addOption("scert", true,
                "The path to your server certificate file.\n The certificate should be in X509v3 format (DER encoding).");

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(90);
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

        // Abort if PSK config is not complete
        if ((cl.hasOption("i") && !cl.hasOption("p")) || !cl.hasOption("i") && cl.hasOption("p")) {
            System.err
                    .println("You should precise identity (-i) and Pre-Shared-Key (-p) if you want to connect in PSK");
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if all RPK config is not complete
        boolean rpkConfig = false;
        if (cl.hasOption("cpubk") || cl.hasOption("spubk")) {
            if (!cl.hasOption("cpubk") || !cl.hasOption("cprik") || !cl.hasOption("spubk")) {
                System.err.println("cpubk, cprik and spubk should be used together to connect using RPK");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                rpkConfig = true;
            }
        }

        // Abort if all X509 config is not complete
        boolean x509config = false;
        if (cl.hasOption("ccert") || cl.hasOption("scert")) {
            if (!cl.hasOption("ccert") || !cl.hasOption("cprik") || !cl.hasOption("scert")) {
                System.err.println("ccert, cprik and scert should be used together to connect using X509");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                x509config = true;
            }
        }

        // Abort if cprik is used without complete RPK or X509 config
        if (cl.hasOption("cprik")) {
            if (!x509config && !rpkConfig) {
                System.err.println(
                        "cprik should be used with ccert and scert for X509 config OR cpubk and spubk for RPK config");
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // Get endpoint name
        String endpoint;
        if (cl.hasOption("n")) {
            endpoint = cl.getOptionValue("n");
        } else {
            try {
                endpoint = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                endpoint = DEFAULT_ENDPOINT;
            }
        }

        // Get lifetime
        int lifetime;
        if (cl.hasOption("l")) {
            lifetime = Integer.parseInt(cl.getOptionValue("l"));
        } else {
            lifetime = DEFAULT_LIFETIME;
        }

        // Get server URI
        String serverURI;
        if (cl.hasOption("u")) {
            if (cl.hasOption("i") || cl.hasOption("cpubk"))
                serverURI = "coaps://" + cl.getOptionValue("u");
            else
                serverURI = "coap://" + cl.getOptionValue("u");
        } else {
            if (cl.hasOption("i") || cl.hasOption("cpubk") || cl.hasOption("ccert"))
                serverURI = "coaps://localhost:" + LwM2m.DEFAULT_COAP_SECURE_PORT;
            else
                serverURI = "coap://localhost:" + LwM2m.DEFAULT_COAP_PORT;
        }

        // get PSK info
        byte[] pskIdentity = null;
        byte[] pskKey = null;
        if (cl.hasOption("i")) {
            pskIdentity = cl.getOptionValue("i").getBytes();
            pskKey = Hex.decodeHex(cl.getOptionValue("p").toCharArray());
        }

        // get RPK info
        PublicKey clientPublicKey = null;
        PrivateKey clientPrivateKey = null;
        PublicKey serverPublicKey = null;
        if (cl.hasOption("cpubk")) {
            try {
                clientPrivateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("cprik"));
                clientPublicKey = SecurityUtil.publicKey.readFromFile(cl.getOptionValue("cpubk"));
                serverPublicKey = SecurityUtil.publicKey.readFromFile(cl.getOptionValue("spubk"));
            } catch (Exception e) {
                System.err.println("Unable to load RPK files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get X509 info
        X509Certificate clientCertificate = null;
        X509Certificate serverCertificate = null;
        if (cl.hasOption("ccert")) {
            try {
                clientPrivateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("cprik"));
                clientCertificate = SecurityUtil.certificate.readFromFile(cl.getOptionValue("ccert"));
                serverCertificate = SecurityUtil.certificate.readFromFile(cl.getOptionValue("scert"));
            } catch (Exception e) {
                System.err.println("Unable to load X509 files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get local address
        String localAddress = null;
        int localPort = 0;
        if (cl.hasOption("lh")) {
            localAddress = cl.getOptionValue("lh");
        }
        if (cl.hasOption("lp")) {
            localPort = Integer.parseInt(cl.getOptionValue("lp"));
        }

        Float latitude = null;
        Float longitude = null;
        Float scaleFactor = 1.0f;
        // get initial Location
        if (cl.hasOption("pos")) {
            try {
                String pos = cl.getOptionValue("pos");
                int colon = pos.indexOf(':');
                if (colon == -1 || colon == 0 || colon == pos.length() - 1) {
                    System.err.println("Position must be a set of two floats separated by a colon, e.g. 48.131:11.459");
                    formatter.printHelp(USAGE, options);
                    return;
                }
                latitude = Float.valueOf(pos.substring(0, colon));
                longitude = Float.valueOf(pos.substring(colon + 1));
            } catch (NumberFormatException e) {
                System.err.println("Position must be a set of two floats separated by a colon, e.g. 48.131:11.459");
                formatter.printHelp(USAGE, options);
                return;
            }
        }
        if (cl.hasOption("sf")) {
            try {
                scaleFactor = Float.valueOf(cl.getOptionValue("sf"));
            } catch (NumberFormatException e) {
                System.err.println("Scale factor must be a float, e.g. 1.0 or 0.01");
                formatter.printHelp(USAGE, options);
                return;
            }
        }
        try {
            createAndStartClient(endpoint, localAddress, localPort, cl.hasOption("b"), lifetime, serverURI, pskIdentity,
                    pskKey, clientPrivateKey, clientPublicKey, serverPublicKey, clientCertificate, serverCertificate,
                    latitude, longitude, scaleFactor);
        } catch (Exception e) {
            System.err.println("Unable to create and start client ...");
            e.printStackTrace();
            return;
        }
    }

    public static void createAndStartClient(String endpoint, String localAddress, int localPort, boolean needBootstrap,
            int lifetime, String serverURI, byte[] pskIdentity, byte[] pskKey, PrivateKey clientPrivateKey,
            PublicKey clientPublicKey, PublicKey serverPublicKey, X509Certificate clientCertificate,
            X509Certificate serverCertificate, Float latitude, Float longitude, float scaleFactor)
            throws CertificateEncodingException {

        locationInstance = new MyLocation((float)56.946285, (float)524.105078, scaleFactor);

        // Initialize model
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models", modelPaths));

        // Initialize object list
        final LwM2mModel model = new StaticModel(models);
        final ObjectsInitializer initializer = new ObjectsInitializer(model);
        if (needBootstrap) {
            if (pskIdentity != null) {
                initializer.setInstancesForObject(SECURITY, pskBootstrap(serverURI, pskIdentity, pskKey));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (clientPublicKey != null) {
                initializer.setInstancesForObject(SECURITY, rpkBootstrap(serverURI, clientPublicKey.getEncoded(),
                        clientPrivateKey.getEncoded(), serverPublicKey.getEncoded()));
                initializer.setClassForObject(SERVER, Server.class);
            } else if (clientCertificate != null) {
                initializer.setInstancesForObject(SECURITY, x509Bootstrap(serverURI, clientCertificate.getEncoded(),
                        clientPrivateKey.getEncoded(), serverCertificate.getEncoded()));
                initializer.setClassForObject(SERVER, Server.class);
            } else {
                initializer.setInstancesForObject(SECURITY, noSecBootstap(serverURI));
                initializer.setClassForObject(SERVER, Server.class);
            }
        } else {
            if (pskIdentity != null) {
                initializer.setInstancesForObject(SECURITY, psk(serverURI, 123, pskIdentity, pskKey));
<<<<<<< HEAD
                initializer.setInstancesForObject(SERVER, new Server(123, 30 * 3, BindingMode.UQ, false));
=======
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
>>>>>>> de4e30bde301f134bcfc499504ca2c9d4a98efae
            } else if (clientPublicKey != null) {
                initializer.setInstancesForObject(SECURITY, rpk(serverURI, 123, clientPublicKey.getEncoded(),
                        clientPrivateKey.getEncoded(), serverPublicKey.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
            } else if (clientCertificate != null) {
                initializer.setInstancesForObject(SECURITY, x509(serverURI, 123, clientCertificate.getEncoded(),
                        clientPrivateKey.getEncoded(), serverCertificate.getEncoded()));
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
            } else {
                initializer.setInstancesForObject(SECURITY, noSec(serverURI, 123));
<<<<<<< HEAD
                initializer.setInstancesForObject(SERVER, new Server(123, 30 * 6, BindingMode.U, false));
=======
                initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
>>>>>>> de4e30bde301f134bcfc499504ca2c9d4a98efae
            }
        }
        initializer.setInstancesForObject(DEVICE, new MyDevice());
        initializer.setInstancesForObject(LOCATION, locationInstance);
        // RandomTemperatureSensor r0 = new RandomTemperatureSensor(); 
        // RandomTemperatureSensor r1 =new RandomTemperatureSensor();
        // initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, r0, r1);
        
         //>>>//additional sensors
        GroupSensors gs = new GroupSensors();
        gs.setGroupSensors("SN00000000001", "SN00000000002", "SN00000000003", "SN00000000004", "SN00000000005", "SN00000000006","SN00000000007","SN00000000008","SN00000000009");
        //gs.setGroupSensors("SN00000000001", "SN00000000002", "SN00000000003");
        initializer.setInstancesForObject(OBJECT_ID_GROUP_DATA, gs);
        initializer.setInstancesForObject(OBJECT_ID_SENSORS, gs.getSensorReadings());
        initializer.setInstancesForObject(OBJECT_ID_SMOKE, gs.getAlarmReadings());
        initializer.setInstancesForObject(OBJECT_ID_DEVICES, gs.getDevices());
        List<LwM2mObjectEnabler> enablers = initializer.createAll();
        //<<<//additional sensors
        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanClientBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setLocalAddress(localAddress, localPort);
        builder.setObjects(enablers);
        builder.setCoapConfig(coapConfig);

        final LeshanClient client = builder.build();
        gs.setLeshanClient(client);

<<<<<<< HEAD
        
=======
        client.getObjectTree().addListener(new ObjectsListenerAdapter() {
            @Override
            public void objectRemoved(LwM2mObjectEnabler object) {
                LOG.info("Object {} disabled.", object.getId());
            }

            @Override
            public void objectAdded(LwM2mObjectEnabler object) {
                LOG.info("Object {} enabled.", object.getId());
            }
        });

>>>>>>> de4e30bde301f134bcfc499504ca2c9d4a98efae
        // Display client public key to easily add it in demo servers.
        if (clientPublicKey != null) {
            PublicKey rawPublicKey = clientPublicKey;
            if (rawPublicKey instanceof ECPublicKey) {
                ECPublicKey ecPublicKey = (ECPublicKey) rawPublicKey;
                // Get x coordinate
                byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
                if (x[0] == 0)
                    x = Arrays.copyOfRange(x, 1, x.length);

                // Get Y coordinate
                byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
                if (y[0] == 0)
                    y = Arrays.copyOfRange(y, 1, y.length);

                // Get Curves params
                String params = ecPublicKey.getParams().toString();

                LOG.info(
                        "Client uses RPK : \n Elliptic Curve parameters  : {} \n Public x coord : {} \n Public y coord : {} \n Public Key (Hex): {} \n Private Key (Hex): {}",
                        params, Hex.encodeHexString(x), Hex.encodeHexString(y),
                        Hex.encodeHexString(rawPublicKey.getEncoded()),
                        Hex.encodeHexString(clientPrivateKey.getEncoded()));

            } else {
                throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");
            }
        }
        // Display X509 credentials to easily at it in demo servers.
        if (clientCertificate != null) {
            LOG.info("Client uses X509 : \n X509 Certificate (Hex): {} \n Private Key (Hex): {}",
                    Hex.encodeHexString(clientCertificate.getEncoded()),
                    Hex.encodeHexString(clientPrivateKey.getEncoded()));
        }

        // Print commands help
        StringBuilder commandsHelp = new StringBuilder("Commands available :");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - create <objectId> : to enable a new object.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - delete <objectId> : to disable a new object.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - update : to trigger a registration update.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - w : to move to North.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - a : to move to East.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - s : to move to South.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append("  - d : to move to West.");
        commandsHelp.append(System.lineSeparator());
        LOG.info(commandsHelp.toString());

        // Start the client
        client.start();
        // De-register on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                client.destroy(true); // send de-registration request before destroy
            }
        });

        // Change the location through the Console
        try (Scanner scanner = new Scanner(System.in)) {
            List<Character> wasdCommands = Arrays.asList('w', 'a', 's', 'd');
            while (scanner.hasNext()) {
                String command = scanner.next();
                if (command.startsWith("create")) {
                    try {
                        int objectId = scanner.nextInt();
                        if (client.getObjectTree().getObjectEnabler(objectId) != null) {
                            LOG.info("Object {} already enabled.", objectId);
                        }
                        if (model.getObjectModel(objectId) == null) {
                            LOG.info("Unable to enable Object {} : there no model for this.", objectId);
                        } else {
                            ObjectsInitializer objectsInitializer = new ObjectsInitializer(model);
                            objectsInitializer.setDummyInstancesForObject(objectId);
                            LwM2mObjectEnabler object = objectsInitializer.create(objectId);
                            client.getObjectTree().addObjectEnabler(object);
                        }
                    } catch (Exception e) {
                        // skip last token
                        scanner.next();
                        LOG.info("Invalid syntax, <objectid> must be an integer : create <objectId>");
                    }
                } else if (command.startsWith("delete")) {
                    try {
                        int objectId = scanner.nextInt();
                        if (objectId == 0 || objectId == 0 || objectId == 3) {
                            LOG.info("Object {} can not be disabled.", objectId);
                        } else if (client.getObjectTree().getObjectEnabler(objectId) == null) {
                            LOG.info("Object {} is not enabled.", objectId);
                        } else {
                            client.getObjectTree().removeObjectEnabler(objectId);
                        }
                    } catch (Exception e) {
                        // skip last token
                        scanner.next();
                        LOG.info("\"Invalid syntax, <objectid> must be an integer : delete <objectId>");
                    }
                } else if (command.startsWith("update")) {
                    client.triggerRegistrationUpdate();
                } else if (command.length() == 1 && wasdCommands.contains(command.charAt(0))) {
                    locationInstance.moveLocation(command);
                } else {
                    LOG.info("Unknown command '{}'", command);
                }
            }
        }
    }
}
