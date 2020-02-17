package org.eclipse.leshan.server.demo.mt.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;

import org.eclipse.leshan.server.demo.mt.scheduler.RequestPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryStorage implements SimpleStorage {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryStorage.class);
    private static final Map<String, Map<String, String>> EP_STORAGE = new ConcurrentHashMap<String, Map<String, String>>();
    private static final Map<String, Map<String, RequestPayload>> REQUEST_STORAGE = new ConcurrentHashMap<String, Map<String, RequestPayload>>();
    private static final Striped<Lock> LOCK_STRIPES = Striped.lock(1);
    private static final int LOCK_TIMEOUT_MS = 100;

    public InMemoryStorage() {
        LOG.warn("Created InMemoryStorage at {}", System.currentTimeMillis());
    }

    @Override
    public String getResource(String endpoint, String resourceLink) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        String resource = null;
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                Map<String, String> objList = EP_STORAGE.get(endpoint);   
                if(objList != null) {
                    resource = objList.get(resourceLink);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return resource;
    }

    @Override
    public void setResource(String endpoint, String resourceLink, String resource) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                Map<String, String> objList = EP_STORAGE.get(endpoint);   
                if(objList == null) {
                    objList = new HashMap<String, String>();
                    EP_STORAGE.put(endpoint, objList);
                }
                objList.put(resourceLink, resource);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteEndpointData(String endpoint) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                EP_STORAGE.remove(endpoint);
                REQUEST_STORAGE.remove(endpoint);
            }                
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public List<RequestPayload> getEndpointRequests(String endpoint) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        List<RequestPayload> result = null;
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                Map<String, RequestPayload> reqMap = REQUEST_STORAGE.get(endpoint);   
                if(reqMap != null && !reqMap.isEmpty()) {
                    result = new ArrayList<RequestPayload>(reqMap.values());
                }
            }
        } catch (InterruptedException e) {
            //returns null
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public void setEndpointRequest(String endpoint, String link, String payload, long timeMs) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                Map<String, RequestPayload> reqMap = REQUEST_STORAGE.get(endpoint);   
                if(reqMap == null) {
                    reqMap = new HashMap<String, RequestPayload>();
                    REQUEST_STORAGE.put(endpoint, reqMap);
                }
                reqMap.put(link, new RequestPayload(link, payload, timeMs));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteEndpointRequest(String endpoint, String link) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                Map<String, RequestPayload> reqMap = REQUEST_STORAGE.get(endpoint);  
                if(reqMap != null) {
                    reqMap.remove(link);
                }
            }                
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}