package org.eclipse.leshan.server.demo.mt.memory;

import java.util.Map;

public interface SimpleStorage {

    String getResource(String endpoint, String resourceLink);

    void setResource(String endpoint, String resourceLink,  String resource);

    void deleteEndpointData(String endpoint);

    Map<String, String> getEndpointRequests(String endpoint);

    void setEndpointRequest(String endpoint, String hashLink, String payload);

    void deleteEndpointRequest(String endpoint, String hashLink);
}