package org.eclipse.leshan.server.demo.mt.memory;

public interface SimpleStorage {

    String getResource(String endpoint, String resourceLink);

    void setResource(String endpoint, String resourceLink,  String resource);

    void deleteEndpointData(String endpoint);
}