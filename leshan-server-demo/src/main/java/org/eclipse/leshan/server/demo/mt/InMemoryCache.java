package org.eclipse.leshan.server.demo.mt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryCache implements SimpleCache {

    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);
    private static final Map<String, String> mEndpointStorage = new ConcurrentHashMap<String, String>();
    private static final Striped<Lock> mStripedLock = Striped.lock(1);

    public InMemoryCache() {
        LOG.warn("Created InMemoryCache at {}", System.currentTimeMillis());
    }

    @Override
    public EndpointCache getEndpointCache(String endpoint) {
        String epc = mEndpointStorage.get(endpoint);
        if (epc != null) {
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

    @Override
    public Boolean lock(String endpoint) {
        return mStripedLock.get(endpoint).tryLock();
    }

    @Override
    public void unlock(String endpoint) {
        mStripedLock.get(endpoint).unlock();
    }
}