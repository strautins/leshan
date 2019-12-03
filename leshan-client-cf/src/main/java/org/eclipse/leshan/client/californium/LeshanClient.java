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
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.client.RegistrationEngine;
import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.californium.bootstrap.BootstrapResource;
import org.eclipse.leshan.client.californium.object.ObjectResource;
import org.eclipse.leshan.client.californium.request.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.observer.LwM2mClientObserverDispatcher;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.Server;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Lightweight M2M client.
 */
public class LeshanClient {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClient.class);

    private final ConcurrentHashMap<Integer, LwM2mObjectEnabler> objectEnablers;

    private final CoapServer clientSideServer;
    private final CaliforniumLwM2mRequestSender requestSender;
    private final RegistrationEngine engine;
    private final BootstrapHandler bootstrapHandler;
    private final LwM2mClientObserverDispatcher observers;

    private final CaliforniumEndpointsManager endpointsManager;

    public LeshanClient(String endpoint, InetSocketAddress localAddress,
            List<? extends LwM2mObjectEnabler> objectEnablers, NetworkConfig coapConfig, Builder dtlsConfigBuilder,
            EndpointFactory endpointFactory, Map<String, String> additionalAttributes, LwM2mNodeEncoder encoder,
            LwM2mNodeDecoder decoder) {

        Validate.notNull(endpoint);
        Validate.notEmpty(objectEnablers);
        Validate.notNull(coapConfig);

        // Create Object enablers
        this.objectEnablers = new ConcurrentHashMap<>();
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            if (this.objectEnablers.containsKey(enabler.getId())) {
                throw new IllegalArgumentException(
                        String.format("There is several objectEnablers with the same id %d.", enabler.getId()));
            }
            this.objectEnablers.put(enabler.getId(), enabler);
        }

        // Create Client Observers
        observers = new LwM2mClientObserverDispatcher();

        bootstrapHandler = new BootstrapHandler(this.objectEnablers);

        // Create CoAP Server
        clientSideServer = new CoapServer(coapConfig) {
            @Override
            protected Resource createRoot() {
                // Use to handle Delete on "/"
                return new org.eclipse.leshan.client.californium.RootResource(bootstrapHandler, this);
            }
        };

        // Create CoAP resources for each lwm2m Objects.
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            ObjectResource clientObject = new ObjectResource(enabler, bootstrapHandler, encoder, decoder);
            clientSideServer.add(clientObject);
        }

        // Create CoAP resources needed for the bootstrap sequence
        clientSideServer.add(new BootstrapResource(bootstrapHandler));

        // Create EndpointHandler
        endpointsManager = new CaliforniumEndpointsManager(clientSideServer, localAddress, coapConfig,
                dtlsConfigBuilder, endpointFactory);

        // Create sender
        requestSender = new CaliforniumLwM2mRequestSender(endpointsManager);

        // Create registration engine
        engine = new RegistrationEngine(endpoint, this.objectEnablers, endpointsManager, requestSender,
                bootstrapHandler, observers, additionalAttributes);

    }

    public void start() {
        LOG.info("Starting Leshan client ...");
        endpointsManager.start();
        engine.start();
        if (LOG.isInfoEnabled()) {
            LOG.info("Leshan client[endpoint:{}] started.", engine.getEndpoint());
        }
    }

    public void stop(boolean deregister) {
        LOG.info("Stopping Leshan Client ...");
        engine.stop(deregister);
        endpointsManager.stop();
        LOG.info("Leshan client stopped.");
    }

    public void destroy(boolean deregister) {
        LOG.info("Destroying Leshan client ...");
        Server currentServer = engine.getServer();
        engine.destroy(deregister);
        if(currentServer != null) {
            Endpoint endpoint = (CoapEndpoint)(endpointsManager.getEndpoint(currentServer.getIdentity()));
            if(endpoint != null && endpoint instanceof CoapEndpoint) {
                Connector connector = ((CoapEndpoint) endpoint).getConnector();
                if (connector instanceof DTLSConnector) {
                    LOG.info("Sending CLOSE_NOTIFY to (DTLS)Coap Server {} ...", currentServer.getIdentity().getPeerAddress());
                    ((DTLSConnector) connector).close(currentServer.getIdentity().getPeerAddress());
                    //time to send CLOSE_NOTIFY. todo: add better wait!
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        endpointsManager.destroy();
        LOG.info("Leshan client destroyed.");
    }

    public void triggerRegistrationUpdate() {
        engine.triggerRegistrationUpdate();
    }

    public Map<Integer, LwM2mObjectEnabler> getObjectEnablers() {
        return Collections.unmodifiableMap(objectEnablers);
    }

    public CoapServer getCoapServer() {
        return clientSideServer;
    }

    public InetSocketAddress getAddress() {
        return endpointsManager.getEndpoint(null).getAddress();
    }

    public void addObserver(LwM2mClientObserver observer) {
        observers.addObserver(observer);
    }

    public void removeObserver(LwM2mClientObserver observer) {
        observers.removeObserver(observer);
    }

    /**
     * Returns the current registration Id (meaningful only because this client implementation supports only one LWM2M
     * server).
     * 
     * @return the client registration Id or <code>null</code> if the client is not registered
     */
    public String getRegistrationId() {
        return engine.getRegistrationId();
    }
}
