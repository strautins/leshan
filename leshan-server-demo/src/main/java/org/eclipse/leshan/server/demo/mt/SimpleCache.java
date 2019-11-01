package org.eclipse.leshan.server.demo.mt;

public interface SimpleCache {
    /** Used to store endpoint cache */
    EndpointCache getEndpointCache(String endpoint);

    void setEndpointCache(String endpoint,  EndpointCache EndpointCache);

    Boolean delEndpointCache(String endpoint);

}