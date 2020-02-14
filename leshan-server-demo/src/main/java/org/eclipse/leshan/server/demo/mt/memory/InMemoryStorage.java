package org.eclipse.leshan.server.demo.mt.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryStorage implements SimpleStorage {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryStorage.class);
    private static final Map<String, Map<String, String>> mEndpointStorage = new ConcurrentHashMap<String, Map<String, String>>();
    private static final Striped<Lock> lockStripes = Striped.lock(1);

    public InMemoryStorage() {
        LOG.warn("Created InMemoryStorage at {}", System.currentTimeMillis());
    }

    @Override
    public String getResource(String endpoint, String resourceLink) {
        Lock lock = lockStripes.get(endpoint);
        try {
            boolean unlock = lock.tryLock(100, TimeUnit.MILLISECONDS);
            if(unlock) {
                Map<String, String> cacheObjList = mEndpointStorage.get(endpoint);   
                if(cacheObjList != null) {
                    return cacheObjList.get(resourceLink);
                }
            }
            return null;
        } catch (InterruptedException e) {
           return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setResource(String endpoint, String resourceLink, String resource) {
        Lock lock = lockStripes.get(endpoint);
        try {
            boolean unlock = lock.tryLock(100, TimeUnit.MILLISECONDS);
            if(unlock) {
                Map<String, String> cacheObjList = mEndpointStorage.get(endpoint);   
                if(cacheObjList == null) {
                    cacheObjList = new HashMap<String, String>();
                    mEndpointStorage.put(endpoint, cacheObjList);
                }
                cacheObjList.put(resourceLink, resource);
            }
        } catch (InterruptedException e) {
            //todo
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteEndpointData(String endpoint) {
        Lock lock = lockStripes.get(endpoint);
        try {
            boolean unlock = lock.tryLock(100, TimeUnit.MILLISECONDS);
            if(unlock) {
                mEndpointStorage.remove(endpoint);
            }                
        } catch (InterruptedException e) {
            //todo
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<String, String> getEndpointRequests(String endpoint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setEndpointRequest(String endpoint, String hashLink, String payload) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteEndpointRequest(String endpoint, String hashLink) {
        // TODO Auto-generated method stub

    }
}