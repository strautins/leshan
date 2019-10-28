package org.eclipse.leshan.server.demo.mt;

public interface EndpointCache {
    /** Used to store endpoint cache */
    String getEndpointCache(String endpoint);

    void setEndpointCache(String endpoint,  String payLoad);

}