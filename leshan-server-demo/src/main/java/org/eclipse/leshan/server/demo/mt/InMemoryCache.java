package org.eclipse.leshan.server.demo.mt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryCache implements EndpointCache {

    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);
    private static final Map<String, String> mEndpointStorage = new ConcurrentHashMap<String,String>();
    
    public InMemoryCache() {
        LOG.warn("Created InMemoryCache at {}", System.currentTimeMillis());
    }

    @Override
    public String getEndpointCache(String endpoint) {
        return mEndpointStorage.get(endpoint);
    }

    @Override
    public void setEndpointCache(String endpoint, String payLoad) {
        mEndpointStorage.put(endpoint, payLoad);

    }
    //todo add cleaner for old data?
}