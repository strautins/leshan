package org.eclipse.leshan.server.demo.mt.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;

import org.eclipse.leshan.server.demo.mt.scheduler.RequestPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class RedisStorage implements SimpleStorage {

    private static final Logger LOG = LoggerFactory.getLogger(RedisStorage.class);
    private static final Striped<Lock> LOCK_STRIPES = Striped.lock(1);
    private static final int LOCK_TIMEOUT_MS = 100;

    private Pool<Jedis> mJedisPool = null;
    // Redis key prefixes
    private static final String REQUEST_EP = "REQUEST:EP:"; /**<EP:hashmap<link:payload>> */
    private static final String REQUEST_QUEUE_EP = "REQUEST:QUEUE:EP:"; /**<EP:hashmap<link:payload_time> */
    private static final String EP_RESOURCE = "EP:RESOURCE:"; /** <EP:hashmap<link:payload>>  */

    //private static final String NX_OPTION = "NX"; // set the key if it does not already exist
    //private static final String PX_OPTION = "PX"; // expire time in millisecond
    
    public RedisStorage(Pool<Jedis> jedisPool) {
        LOG.warn("Created RedisStorage at {}", System.currentTimeMillis());
        this.mJedisPool = jedisPool;
    }

    private String getKey(String key, String endpoint) {
        return key + endpoint;
    }

    @Override
    public String getResource(String endpoint, String resourceLink) {
        try (Jedis jedis = mJedisPool.getResource()) {
            resourceLink = resourceLink.replace("/", ":");
            return jedis.hget(getKey(EP_RESOURCE, endpoint), resourceLink);  
        }
    }

    @Override
    public void setResource(String endpoint, String resourceLink,  String resource) {
        try (Jedis jedis = mJedisPool.getResource()) {
            resourceLink = resourceLink.replace("/", ":");
            jedis.hset(getKey(EP_RESOURCE, endpoint), resourceLink, resource);      
        }
    }
    
    @Override
    public void deleteEndpointData(String endpoint) {
        try (Jedis jedis = mJedisPool.getResource()) {
            jedis.del(getKey(EP_RESOURCE, endpoint));      
            jedis.del(getKey(REQUEST_EP, endpoint));  
            jedis.del(getKey(REQUEST_QUEUE_EP, endpoint));      
        }
    }

    public List<RequestPayload> getEndpointRequests(String endpoint) {
        List<RequestPayload> payLoadList = null;
        Lock lock = LOCK_STRIPES.get(endpoint);
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                try (Jedis jedis = mJedisPool.getResource()) {
                    Map<String, String> payLoadMap = jedis.hgetAll(getKey(REQUEST_EP, endpoint));
                    Map<String, String> payLoadQueueMap = jedis.hgetAll(getKey(REQUEST_QUEUE_EP, endpoint));
                    if(payLoadMap != null && payLoadQueueMap != null && payLoadMap.size() == payLoadQueueMap.size()) {
                        payLoadList = new ArrayList<RequestPayload>();
                        for(Map.Entry<String, String> entry : payLoadMap.entrySet()) {
                            payLoadList.add(new RequestPayload(entry.getKey(), entry.getValue(), Long.parseLong(payLoadQueueMap.get(entry.getKey()))));
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return payLoadList;
    }

    public void setEndpointRequest(String endpoint, String link, String payload, long timeMs) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                try (Jedis jedis = mJedisPool.getResource()) {
                    jedis.hset(getKey(REQUEST_EP, endpoint), link, payload);
                    jedis.hset(getKey(REQUEST_QUEUE_EP, endpoint), link, Long.toString(timeMs));
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void deleteEndpointRequest(String endpoint, String hashLink) {
        Lock lock = LOCK_STRIPES.get(endpoint);
        try {
            boolean unlock = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if(unlock) {
                try (Jedis jedis = mJedisPool.getResource()) {
                    jedis.hdel(getKey(REQUEST_EP, endpoint), hashLink);
                    jedis.hdel(getKey(REQUEST_QUEUE_EP, endpoint), hashLink);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}