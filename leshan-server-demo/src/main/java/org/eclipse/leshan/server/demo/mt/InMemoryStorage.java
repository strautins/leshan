package org.eclipse.leshan.server.demo.mt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryStorage implements SimpleStorage {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryStorage.class);
    private static final Map<String, String> mEndpointStorage = new ConcurrentHashMap<String, String>();

    public InMemoryStorage() {
        LOG.warn("Created InMemoryStorage at {}", System.currentTimeMillis());
    }

    @Override
    public String getResource(String endpoint, String resourceLink) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setResource(String endpoint, String resourceLink, String resource) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteEndpointData(String endpoint) {
        // TODO Auto-generated method stub
    }
}