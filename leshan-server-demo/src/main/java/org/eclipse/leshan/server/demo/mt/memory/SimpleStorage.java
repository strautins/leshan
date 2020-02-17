package org.eclipse.leshan.server.demo.mt.memory;

import java.util.List;

import org.eclipse.leshan.server.demo.mt.scheduler.RequestPayload;

public interface SimpleStorage {

    String getResource(String endpoint, String resourceLink);

    void setResource(String endpoint, String resourceLink,  String resource);

    void deleteEndpointData(String endpoint);

    List<RequestPayload> getEndpointRequests(String endpoint);

    void setEndpointRequest(String endpoint, String link, String payload, long timeMs);

    void deleteEndpointRequest(String endpoint, String link);
}