package org.eclipse.leshan.server.demo.mt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryCache implements SimpleCache {

    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);
    private static final Map<String, String> mEndpointStorage = new ConcurrentHashMap<String,String>();
    
    public InMemoryCache() {
        LOG.warn("Created InMemoryCache at {}", System.currentTimeMillis());
    }

    @Override
    public EndpointCache getEndpointCache(String endpoint) {
        String epc = mEndpointStorage.get(endpoint);
        if(epc != null) {
            return new EndpointCache(endpoint, epc);    
        }
        return null;
    }

    @Override
    public void setEndpointCache(String endpoint, EndpointCache endpointCache) {
        mEndpointStorage.put(endpoint, endpointCache.toPayload());
    }
    @Override
    public Boolean delEndpointCache(String endpoint) {
        String s = mEndpointStorage.remove(endpoint);
        return s == null ? false : true;
    }
}